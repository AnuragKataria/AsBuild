package com.rbt.survey.ui.locationTrackingDashboard

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Polyline
import com.rbt.survey.data.utils.bitmapDescriptorFromVector
import com.rbt.survey.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTrackingScreen(
    viewModel: LocationTrackingViewModel,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(20.5937, 78.9629),
            4f
        )
    }

    var isFilterExpanded by remember {
        mutableStateOf(false)
    }

    // DEFAULT VALUES
    var selectedUser by remember {
        mutableStateOf("Select User")
    }

    var selectedType by remember {
        mutableStateOf("Select Type")
    }

    var fromDate by remember {
        mutableStateOf("")
    }

    var toDate by remember {
        mutableStateOf("")
    }

    var selectedUserId by remember {
        mutableStateOf<Int?>(null)
    }

    var isUserDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var isTypeDropdownExpanded by remember {
        mutableStateOf(false)
    }

    val allUsersLocations by viewModel
        .allUsersLocations
        .collectAsState()

    val usersList by viewModel
        .usersList
        .collectAsState()

    val latestUserLocation by viewModel
        .latestUserLocation
        .collectAsState()

    val historyLocations by viewModel
        .historyLocations
        .collectAsState()

    val loading by viewModel
        .loading
        .collectAsState()

    val emptyMessage by viewModel
        .emptyMessage
        .collectAsState()

    LaunchedEffect(emptyMessage) {

        emptyMessage?.let {

            Toast
                .makeText(
                    context,
                    it,
                    android.widget.Toast.LENGTH_SHORT
                )
                .show()

            viewModel.clearToastMessage()
        }
    }

    // API CALL FIRST TIME
    LaunchedEffect(Unit) {

        viewModel.getAllUsersLocations(false)
    }

    val typeList = listOf(
        "Select Type",
        "Latest",
        "History"
    )

    val isApplyEnabled =

        // ALL USERS
        selectedUser == "ALL" ||

                // LATEST
                (
                        selectedUserId != null &&
                                selectedType == "Latest"
                        ) ||

                // HISTORY
                (
                        selectedUserId != null &&
                                selectedType == "History" &&
                                fromDate.isNotEmpty() &&
                                toDate.isNotEmpty()
                        )

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {
                    Text("Location Tracking")
                },

                navigationIcon = {

                    IconButton(
                        onClick = { onBack() }
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // OPEN CLOSE
            Text(

                text =
                    if (isFilterExpanded)
                        "Click to close"
                    else
                        "Click to open",

                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isFilterExpanded = !isFilterExpanded
                    }
                    .background(Color.White)
                    .padding(vertical = 4.dp),

                textAlign = TextAlign.Center,

                color = Color.Black,

                fontWeight = FontWeight.Medium
            )

            // FILTER AREA
            AnimatedVisibility(
                visible = isFilterExpanded
            ) {

                Column(

                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                ) {

                    // USER DROPDOWN
                    ExposedDropdownMenuBox(

                        expanded = isUserDropdownExpanded,

                        onExpandedChange = {
                            isUserDropdownExpanded = !isUserDropdownExpanded
                        }
                    ) {

                        OutlinedTextField(

                            value = selectedUser,

                            onValueChange = {},

                            readOnly = true,

                            label = {
                                Text("Select User")
                            },

                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = isUserDropdownExpanded
                                )
                            },

                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(

                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,

                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,

                                focusedBorderColor = Color.Gray,
                                unfocusedBorderColor = Color.LightGray
                            ),
                        )

                        ExposedDropdownMenu(

                            expanded = isUserDropdownExpanded,

                            onDismissRequest = {
                                isUserDropdownExpanded = false
                            }
                        ) {

                            DropdownMenuItem(

                                text = {
                                    Text("Select User")
                                },

                                onClick = {

                                    selectedUser = "Select User"
                                    selectedUserId = null

                                    isUserDropdownExpanded = false
                                }
                            )

                            DropdownMenuItem(

                                text = {
                                    Text("ALL")
                                },

                                onClick = {

                                    selectedUser = "ALL"
                                    selectedUserId = null

                                    isUserDropdownExpanded = false
                                }
                            )

                            usersList.forEach { item ->

                                DropdownMenuItem(

                                    text = {
                                        Text(item.userName ?: "")
                                    },

                                    onClick = {

                                        selectedUser = item.userName ?: ""
                                        selectedUserId = item.userId

                                        selectedType="Select Type"
                                        fromDate=""
                                        toDate=""

                                        isUserDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // SHOW TYPE ONLY WHEN USER SELECTED
                    if (
                        selectedUser != "Select User" &&
                        selectedUser != "ALL"
                    ) {

                        Spacer(modifier = Modifier.height(12.dp))

                        // TYPE DROPDOWN
                        ExposedDropdownMenuBox(

                            expanded = isTypeDropdownExpanded,

                            onExpandedChange = {
                                isTypeDropdownExpanded = !isTypeDropdownExpanded
                            }
                        ) {

                            OutlinedTextField(

                                value = selectedType,

                                onValueChange = {},

                                readOnly = true,

                                label = {
                                    Text("Select Type")
                                },

                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = isTypeDropdownExpanded
                                    )
                                },

                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),

                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(

                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,

                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,

                                    focusedBorderColor = Color.Gray,
                                    unfocusedBorderColor = Color.LightGray
                                ),
                            )

                            ExposedDropdownMenu(

                                expanded = isTypeDropdownExpanded,

                                onDismissRequest = {
                                    isTypeDropdownExpanded = false
                                }
                            ) {

                                typeList.forEach { type ->

                                    DropdownMenuItem(

                                        text = {
                                            Text(type)
                                        },

                                        onClick = {

                                            selectedType = type
                                            fromDate = ""
                                            toDate = ""

                                            isTypeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // HISTORY DATE PICKER
                        if (selectedType == "History") {

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                // FROM DATE
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    OutlinedTextField(

                                        value = fromDate,

                                        onValueChange = {},

                                        readOnly = true,

                                        enabled = false,

                                        singleLine = true,

                                        label = {
                                            Text("From Date")
                                        },

                                        modifier = Modifier
                                            .fillMaxWidth(),

                                        shape = RoundedCornerShape(12.dp),

                                        colors = OutlinedTextFieldDefaults.colors(

                                            disabledTextColor = Color.Black,
                                            disabledBorderColor = Color.LightGray,
                                            disabledContainerColor = Color.White,
                                            disabledLabelColor = Color.Gray
                                        )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable {

                                                val calendar = Calendar.getInstance()

                                                val datePicker = DatePickerDialog(
                                                    context,
                                                    { _, year, month, day ->

                                                        fromDate = String.format(
                                                            "%04d-%02d-%02d",
                                                            year,
                                                            month + 1,
                                                            day
                                                        )
                                                        toDate = ""
                                                    },
                                                    calendar.get(Calendar.YEAR),
                                                    calendar.get(Calendar.MONTH),
                                                    calendar.get(Calendar.DAY_OF_MONTH)
                                                )

                                                datePicker.datePicker.maxDate = System.currentTimeMillis()

                                                datePicker.show()
                                            }
                                    )
                                }

                                // TO DATE
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    OutlinedTextField(

                                        value = toDate,

                                        onValueChange = {},

                                        readOnly = true,

                                        enabled = false,

                                        singleLine = true,

                                        label = {
                                            Text("To Date")
                                        },

                                        modifier = Modifier
                                            .fillMaxWidth(),

                                        shape = RoundedCornerShape(12.dp),

                                        colors = OutlinedTextFieldDefaults.colors(

                                            disabledTextColor = Color.Black,
                                            disabledBorderColor = Color.LightGray,
                                            disabledContainerColor = Color.White,
                                            disabledLabelColor = Color.Gray
                                        )
                                    )

                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable (

                                                enabled = fromDate.isNotEmpty()

                                            ){

                                                val calendar = Calendar.getInstance()

                                                val datePicker = DatePickerDialog(
                                                    context,
                                                    { _, year, month, day ->

                                                        toDate = String.format(
                                                            "%04d-%02d-%02d",
                                                            year,
                                                            month + 1,
                                                            day
                                                        )
                                                    },
                                                    calendar.get(Calendar.YEAR),
                                                    calendar.get(Calendar.MONTH),
                                                    calendar.get(Calendar.DAY_OF_MONTH)
                                                )

                                                // NO FUTURE DATE
                                                datePicker.datePicker.maxDate = System.currentTimeMillis()

                                                // CANNOT SELECT BEFORE FROM DATE

                                                val sdf = SimpleDateFormat(
                                                    "yyyy-MM-dd",
                                                    Locale.getDefault()
                                                )

                                                val fromDateMillis = sdf.parse(fromDate)?.time ?: 0L

                                                datePicker.datePicker.minDate = fromDateMillis

                                                datePicker.show()
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // BUTTONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {

                        // CLEAR
                        OutlinedButton(

                            onClick = {

                                selectedUser = "Select User"
                                selectedType = "Select Type"

                                selectedUserId = null

                                fromDate = ""
                                toDate = ""

                                viewModel.clearData()
                            }
                        ) {

                            Text("Clear")
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // APPLY
                        Button(
                            enabled = isApplyEnabled,
                            colors = ButtonDefaults.buttonColors(

                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.DarkGray
                            ),
                            onClick = {
                                isFilterExpanded = false

                                // ALL USERS
                                if (selectedUser == "ALL") {

                                    viewModel.getAllUsersLocations(true)
                                }

                                // LATEST
                                else if (
                                    selectedType == "Latest" &&
                                    selectedUserId != null
                                ) {

                                    viewModel.getLatestUserLocation(
                                        selectedUserId!!
                                    )
                                }

                                // HISTORY
                                else if (
                                    selectedType == "History" &&
                                    selectedUserId != null &&
                                    fromDate.isNotEmpty() &&
                                    toDate.isNotEmpty()
                                ) {

                                    val from =
                                        "${fromDate}T00:00:00Z"

                                    val to =
                                        "${toDate}T23:59:59Z"

                                    viewModel.getUserLocationHistory(
                                        userId = selectedUserId!!,
                                        from = from,
                                        to = to
                                    )
                                }
                            }
                        ) {

                            Text("Apply")
                        }
                    }
                }
            }

            // MAP
            Box(
                modifier = Modifier.weight(1f)
            ) {

                GoogleMap(

                    modifier = Modifier.fillMaxSize(),

                    cameraPositionState = cameraPositionState,

                    properties = MapProperties(
                        isMyLocationEnabled = true
                    ),

                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true
                    )
                ){

                    allUsersLocations.forEach { item ->

                        val position = LatLng(
                            item.lat ?: 0.0,
                            item.lng ?: 0.0
                        )

                        val iconRes = when (item.status) {

                            "online" -> R.drawable.ic_user_online
                            "offline" -> R.drawable.ic_user_offline
                            else -> R.drawable.ic_user_idle
                        }

                        val markerIcon = bitmapDescriptorFromVector(
                            context,
                            iconRes
                        )

                        Marker(

                            state = MarkerState(position = position),

                            title = item.userName,

                            icon = markerIcon
                        )
                    }

                    latestUserLocation?.let { item ->

                        val position = LatLng(
                            item.lat ?: 0.0,
                            item.lng ?: 0.0
                        )

                        // MOVE CAMERA
                        LaunchedEffect(position) {

                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(
                                    position,
                                    16f
                                )
                        }

                        val markerIcon = bitmapDescriptorFromVector(
                            context,
                            R.drawable.ic_user_online
                        )

                        Marker(

                            state = MarkerState(position = position),

                            title = item.userName,

                            icon = markerIcon
                        )
                    }

                    // HISTORY LOCATION
                    val historyPoints = historyLocations.map {

                        LatLng(
                            it.lat ?: 0.0,
                            it.lng ?: 0.0
                        )
                    }

                    // POLYLINE
                    if (historyPoints.isNotEmpty()) {

                        Polyline(

                            points = historyPoints,

                            color = Color.Blue,

                            width = 8f
                        )
                    }

                    val historyMarkerBitmap = createSquareMarker(

                        color = android.graphics.Color.RED,

                        size = 25
                    )

                    // HISTORY POINT MARKERS
                    historyPoints.forEach { point ->

                        Marker(

                            state = MarkerState(position = point),

                            icon = BitmapDescriptorFactory
                                .fromBitmap(historyMarkerBitmap)
                        )
                    }

                    // AUTO ZOOM ROUTE
                    LaunchedEffect(historyPoints) {

                        if (historyPoints.isNotEmpty()) {

                            val boundsBuilder = LatLngBounds.Builder()

                            historyPoints.forEach {
                                boundsBuilder.include(it)
                            }

                            cameraPositionState.move(

                                com.google.android.gms.maps.CameraUpdateFactory
                                    .newLatLngBounds(
                                        boundsBuilder.build(),
                                        120
                                    )
                            )
                        }
                    }
                }

                if (loading) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable(
                                enabled = true,
                                onClick = {}
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

fun createSquareMarker(
    color: Int,
    size: Int
): Bitmap {

    val bitmap = Bitmap.createBitmap(
        size,
        size,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
    }

    canvas.drawRect(
        0f,
        0f,
        size.toFloat(),
        size.toFloat(),
        paint
    )

    return bitmap
}