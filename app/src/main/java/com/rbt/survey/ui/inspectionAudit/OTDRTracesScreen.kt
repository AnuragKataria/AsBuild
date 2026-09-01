package com.rbt.survey.ui.inspectionAudit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.google.gson.JsonObject
import com.rbt.survey.ui.incidentManagement.IncidentProjectDropdown
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream
import com.rbt.survey.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTDRTracesScreen(
    viewModel: OTDRTracesViewModel,
    onBack: () -> Unit,
) {

    val context = LocalContext.current

    val projects by viewModel.projects.collectAsState()
    val isUploading by viewModel.isLoading.collectAsState()
    val isLoadingSites by viewModel.isLoadingSites.collectAsState()
    val loadingmessage by viewModel.isLoadingmessage.collectAsState()
    val otdrReports by viewModel.otdrReports.collectAsState()
    val siteNames by viewModel.siteNames.collectAsState()

    var selectedProject by remember { mutableStateOf<ProjectResponse?>(null) }

    val projectSelected = selectedProject != null

    val deviceTypes = listOf(
        "EXFO",
        "VIAVI"
    )

    // -----------------------------
    // UI STATES
    // -----------------------------

    var uploadExpanded by remember { mutableStateOf(false) }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var deviceExpanded by remember { mutableStateOf(false) }
    var selectedFrom by remember { mutableStateOf<String?>(null) }
    var selectedTo by remember { mutableStateOf<String?>(null) }
    var selectedDevice by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var fileError by remember { mutableStateOf<String?>(null) }
    var fromSearch by remember { mutableStateOf("") }
    var toSearch by remember { mutableStateOf("") }

    val fromOptions = siteNames
        .filter {
            it != selectedTo
        }
        .filter {
            fromSearch.isBlank() ||
                    it.contains(
                        fromSearch,
                        ignoreCase = true
                    )
        }

    val toOptions = siteNames
        .filter {
            it != selectedFrom
        }
        .filter {
            toSearch.isBlank() ||
                    it.contains(
                        toSearch,
                        ignoreCase = true
                    )
        }

    // -----------------------------
    // FILE PICKER
    // -----------------------------

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            fileError = null

            val fileName = getFileName(
                context,
                uri
            )

            if (fileName.isNullOrBlank()) {

                selectedFileUri = null
                selectedFileName = ""

                fileError =
                    "Unable to read selected file."

                return@rememberLauncherForActivityResult
            }

            val extension =
                fileName
                    .substringAfterLast(
                        ".",
                        ""
                    )
                    .lowercase()

            when (extension) {

                // -------------------------
                // PDF
                // -------------------------

                "pdf" -> {

                    selectedFileUri = uri
                    selectedFileName = fileName
                    fileError = null
                }

                // -------------------------
                // ZIP
                // -------------------------

                "zip" -> {

                    val validation =
                        validateZipFile(
                            context = context,
                            uri = uri
                        )

                    if (validation.isValid) {

                        selectedFileUri = uri
                        selectedFileName = fileName
                        fileError = null

                    } else {

                        selectedFileUri = null
                        selectedFileName = ""

                        fileError =
                            validation.errorMessage
                    }
                }

                // -------------------------
                // OTHER FILE
                // -------------------------

                else -> {

                    selectedFileUri = null
                    selectedFileName = ""

                    fileError =
                        "Only PDF or ZIP files are allowed."
                }
            }
        }

    LaunchedEffect(Unit) {
        viewModel.loadOTDRReports()
        viewModel.loadFilterProjects()
    }

    LaunchedEffect(Unit) {

        viewModel.uploadSuccess.collect { success ->

            if (success) {

                uploadExpanded = false

                selectedFrom = null
                selectedTo = null
                selectedDevice = null
                selectedFileUri = null
                selectedFileName = ""
                fileError = null

                viewModel.loadOTDRReports()

                Toast.makeText(
                    context,
                    "OTDR Trace uploaded successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }else{

                Toast.makeText(
                    context,
                    "Failed to upload OTDR Trace",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }


    // -----------------------------
    // SCREEN
    // -----------------------------

    Scaffold(

        topBar = {

            CenterAlignedTopAppBar(

                title = {
                    Text(
                        text = "OTDR Traces",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                // =================================
                // UPLOAD TRACE CARD
                // =================================

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(16.dp),

                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                ) {

                    Column {

                        // -----------------------------
                        // HEADER
                        // -----------------------------

                        Row(

                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {

                                    uploadExpanded =
                                        !uploadExpanded

                                }
                                .padding(16.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(

                                imageVector =
                                    if (uploadExpanded)
                                        Icons.Default.KeyboardArrowUp
                                    else
                                        Icons.Default.KeyboardArrowDown,

                                contentDescription =
                                    null
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(

                                text =
                                    "Upload OTDR Trace",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        // =================================
                        // EXPANDED SECTION
                        // =================================

                        AnimatedVisibility(
                            visible = uploadExpanded
                        ) {

                            Column(

                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp
                                    )
                                    .padding(
                                        bottom = 16.dp
                                    ),

                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {

                                OTDRProjectDropdown(
                                    projects = projects,
                                    selectedProject = selectedProject,
                                    onSelected = { project ->

                                        selectedProject = project

                                        selectedFrom = null
                                        selectedTo = null

                                        viewModel.loadOTDRSiteNames(
                                            projectId = project.projectId
                                        )
                                    }
                                )

                                // FROM DROPDOWN

                                OTDRStationDropdown(
                                    label = "From *",
                                    selectedStation = selectedFrom,
                                    stations = siteNames,
                                    excludedStation = selectedTo,
                                    isLoading = isLoadingSites,
                                    loadingMessage = loadingmessage,
                                    searchPlaceholder = "Search From station...",
                                    enabled = selectedProject != null,
                                    onSelected = { station ->

                                        selectedFrom = station

                                        // Safety check
                                        if (selectedTo == station) {
                                            selectedTo = null
                                        }
                                    }
                                )

                                // TO DROPDOWN

                                OTDRStationDropdown(
                                    label = "To *",
                                    selectedStation = selectedTo,
                                    stations = siteNames,
                                    excludedStation = selectedFrom,
                                    isLoading = isLoadingSites,
                                    loadingMessage = loadingmessage,
                                    searchPlaceholder = "Search To station...",
                                    enabled = selectedProject != null,
                                    onSelected = { station ->

                                        selectedTo = station

                                        // Safety check
                                        if (selectedFrom == station) {
                                            selectedFrom = null
                                        }
                                    }
                                )

                                // =================================
                                // DEVICE TYPE
                                // =================================

                                ExposedDropdownMenuBox(

                                    expanded =
                                        deviceExpanded,

                                    onExpandedChange = {

                                        deviceExpanded =
                                            !deviceExpanded
                                    }
                                ) {

                                    OutlinedTextField(

                                        value =
                                            selectedDevice ?: "",

                                        onValueChange = {},

                                        readOnly = true,

                                        label = {
                                            Text(
                                                "Device Type *"
                                            )
                                        },

                                        placeholder = {
                                            Text(
                                                "Select Device Type"
                                            )
                                        },

                                        trailingIcon = {

                                            ExposedDropdownMenuDefaults
                                                .TrailingIcon(
                                                    expanded =
                                                        deviceExpanded
                                                )
                                        },

                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(

                                        expanded =
                                            deviceExpanded,

                                        onDismissRequest = {

                                            deviceExpanded =
                                                false
                                        }
                                    ) {

                                        deviceTypes.forEach { device ->

                                            DropdownMenuItem(

                                                text = {
                                                    Text(device)
                                                },

                                                onClick = {

                                                    selectedDevice =
                                                        device

                                                    deviceExpanded =
                                                        false
                                                }
                                            )
                                        }
                                    }
                                }

                                // =================================
                                // FILE
                                // =================================

                                Text(
                                    text = "Trace File *",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelLarge
                                )

                                OutlinedCard(

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {

                                                filePickerLauncher
                                                    .launch(
                                                        arrayOf(
                                                            "application/pdf",
                                                            "application/zip",
                                                            "application/x-zip-compressed"
                                                        )
                                                    )
                                            }
                                ) {

                                    Row(

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),

                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.AttachFile,

                                            contentDescription =
                                                null
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.width(10.dp)
                                        )

                                        Column(
                                            modifier =
                                                Modifier.weight(1f)
                                        ) {

                                            Text(

                                                text =
                                                    if (
                                                        selectedFileName
                                                            .isBlank()
                                                    ) {
                                                        "Select PDF or ZIP file"
                                                    } else {
                                                        selectedFileName
                                                    },

                                                maxLines = 1,

                                                overflow =
                                                    TextOverflow.Ellipsis
                                            )

                                            if (
                                                selectedFileName
                                                    .isBlank()
                                            ) {

                                                Text(

                                                    text =
                                                        "Only PDF or ZIP files allowed",

                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .bodySmall,

                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurfaceVariant
                                                )
                                            }
                                        }

                                        TextButton(

                                            onClick = {

                                                filePickerLauncher
                                                    .launch(
                                                        arrayOf(
                                                            "application/pdf",
                                                            "application/zip",
                                                            "application/x-zip-compressed"
                                                        )
                                                    )
                                            }
                                        ) {

                                            Text("Browse")
                                        }
                                    }
                                }

                                // =================================
                                // FILE ERROR
                                // =================================

                                if (
                                    fileError != null
                                ) {

                                    Text(

                                        text =
                                            fileError!!,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .error,

                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }

                                // =================================
                                // UPLOAD
                                // =================================

                                Button(

                                    onClick = {

                                        val uri = selectedFileUri ?: return@Button

                                        val projectId =
                                            selectedProject?.projectId
                                                ?: return@Button

                                        val file = uriToFile(
                                            context = context,
                                            uri = uri
                                        )

                                        viewModel.uploadTrace(
                                            file = file,
                                            projectId = projectId,
                                            fromStation = selectedFrom.orEmpty(),
                                            toStation = selectedTo.orEmpty(),
                                            deviceType = selectedDevice.orEmpty()
                                        )

                                    },

                                    enabled =
                                        selectedProject != null &&
                                        selectedFrom != null &&
                                                selectedTo != null &&
                                                selectedDevice != null &&
                                                selectedFileUri != null &&
                                                !isUploading,

                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.CloudUpload,

                                        contentDescription =
                                            null
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text("Upload")

                                }
                            }
                        }
                    }
                }
                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Text(
                    text = "OTDR Trace Reports",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                // =================================
                // OTDR TRACE REPORT LIST
                // =================================

                if (otdrReports.isEmpty()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "No OTDR traces found",

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                } else {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp),

                        contentPadding =
                            PaddingValues(bottom = 16.dp)
                    ) {

                        items(
                            items = otdrReports,

                            key = {
                                it.get(
                                    "otdrReportUploadId"
                                )?.asInt ?: 0
                            }
                        ) { report ->

                            OTDRReportCard(
                                report = report
                            )
                        }
                    }
                }
            }
            if (isUploading) {

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Color.Black.copy(alpha = 0.4f)
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {

                        },
                    contentAlignment = Alignment.Center
                ) {

                    Card(
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 20.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            CircularProgressIndicator()

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = loadingmessage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OTDRProjectDropdown(
    projects: List<ProjectResponse>,
    selectedProject: ProjectResponse?,
    onSelected: (ProjectResponse) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Project *",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedTextField(
                value = selectedProject?.projectName ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Select Project")
                },
                trailingIcon = {

                    Icon(
                        imageVector =
                            if (expanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        expanded = !expanded
                    }
            )
        }

        if (expanded) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(top = 4.dp)
            ) {

                LazyColumn {

                    items(
                        items = projects,
                        key = { it.projectId }
                    ) { project ->

                        DropdownMenuItem(

                            text = {
                                Text(project.projectName)
                            },

                            onClick = {

                                onSelected(project)

                                expanded = false
                            }
                        )

                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun OTDRStationDropdown(
    label: String,
    selectedStation: String?,
    stations: List<String>,
    excludedStation: String?,
    isLoading: Boolean,
    loadingMessage: String,
    searchPlaceholder: String,
    enabled: Boolean = true,
    onSelected: (String?) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    // -----------------------------------------
    // FILTER STATIONS
    // -----------------------------------------

    val filteredStations = remember(
        stations,
        excludedStation,
        searchQuery
    ) {

        stations
            .filter { station ->
                station != excludedStation
            }
            .filter { station ->

                searchQuery.isBlank() ||
                        station.contains(
                            searchQuery,
                            ignoreCase = true
                        )
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        // -----------------------------------------
        // LABEL
        // -----------------------------------------

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // -----------------------------------------
        // SELECTED STATION FIELD
        // -----------------------------------------

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedTextField(
                value = when {

                    isLoading ->
                        "Loading stations..."

                    selectedStation != null ->
                        selectedStation

                    else ->
                        "Select station"
                },

                onValueChange = {},

                readOnly = true,

                enabled = enabled && !isLoading,

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                shape = RoundedCornerShape(10.dp),

                trailingIcon = {

                    Icon(
                        imageVector =
                            if (expanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },

                        contentDescription = null
                    )
                },

                colors = OutlinedTextFieldDefaults.colors(

                    disabledTextColor =
                        MaterialTheme.colorScheme.onSurface,

                    disabledBorderColor =
                        MaterialTheme.colorScheme.outline,

                    disabledTrailingIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // -----------------------------------------
            // CLICK OVERLAY
            // -----------------------------------------

            if (enabled && !isLoading) {

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {

                            expanded = !expanded

                        }
                )
            }
        }

        // -----------------------------------------
        // DROPDOWN CARD
        // -----------------------------------------

        if (expanded) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(top = 4.dp),

                shape = RoundedCornerShape(10.dp),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // -----------------------------------------
                    // SEARCH
                    // -----------------------------------------

                    OutlinedTextField(
                        value = searchQuery,

                        onValueChange = {
                            searchQuery = it
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),

                        singleLine = true,

                        placeholder = {
                            Text(searchPlaceholder)
                        },

                        leadingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,

                                contentDescription =
                                    "Search"
                            )
                        },

                        trailingIcon = {

                            if (searchQuery.isNotEmpty()) {

                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Clear,

                                        contentDescription =
                                            "Clear"
                                    )
                                }
                            }
                        },

                        shape = RoundedCornerShape(10.dp)
                    )

                    HorizontalDivider()

                    // -----------------------------------------
                    // LOADING
                    // -----------------------------------------

                    if (isLoading) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize(),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                CircularProgressIndicator()

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(
                                    text =
                                        loadingMessage.ifBlank {
                                            "Loading stations..."
                                        }
                                )
                            }
                        }

                    }

                    // -----------------------------------------
                    // NO DATA
                    // -----------------------------------------

                    else if (filteredStations.isEmpty()) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize(),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    if (searchQuery.isBlank()) {
                                        "No stations available"
                                    } else {
                                        "No matching stations"
                                    },

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }

                    }

                    // -----------------------------------------
                    // STATION LIST
                    // -----------------------------------------

                    else {

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),

                            contentPadding =
                                PaddingValues(
                                    bottom = 8.dp
                                )
                        ) {

                            items(
                                items = filteredStations,
                                key = { station ->
                                    station
                                }
                            ) { station ->

                                DropdownMenuItem(

                                    text = {

                                        Text(
                                            text = station,

                                            fontWeight =
                                                if (
                                                    selectedStation ==
                                                    station
                                                ) {
                                                    FontWeight.SemiBold
                                                } else {
                                                    FontWeight.Normal
                                                }
                                        )
                                    },

                                    trailingIcon = {

                                        if (
                                            selectedStation ==
                                            station
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Default.Check,

                                                contentDescription =
                                                    null
                                            )
                                        }
                                    },

                                    onClick = {

                                        onSelected(station)

                                        searchQuery = ""

                                        expanded = false
                                    }
                                )

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OTDRReportCard(
    report: JsonObject
) {

    val fromStation =
        report.get("fromStationName")
            ?.asString
            .orEmpty()

    val toStation =
        report.get("toStationName")
            ?.asString
            .orEmpty()

    val deviceType =
        report.get("otdrDeviceType")
            ?.asString
            .orEmpty()

    val fileName =
        report.get("reportFileName")
            ?.asString
            .orEmpty()

    val uploadDate =
        report.get("reportUploadDate")
            ?.asString
            .orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // -------------------------
            // FILE NAME + DEVICE
            // -------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = fileName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme
                        .colorScheme
                        .primaryContainer
                ) {

                    Text(
                        text = deviceType,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),
                        style = MaterialTheme
                            .typography
                            .labelMedium
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // -------------------------
            // FROM
            // -------------------------

            OTDRReportInfoRow(
                label = "From",
                value = fromStation
            )

            // -------------------------
            // TO
            // -------------------------

            OTDRReportInfoRow(
                label = "To",
                value = toStation
            )

            // -------------------------
            // DATE
            // -------------------------

            OTDRReportInfoRow(
                label = "Date",
                value = uploadDate
            )
        }
    }
}

@Composable
fun OTDRReportInfoRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {

        Text(
            text = label,
            modifier = Modifier.width(100.dp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme
                .colorScheme
                .onSurfaceVariant
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getFileName(
    context: Context,
    uri: Uri
): String? {

    var fileName: String? = null

    context.contentResolver.query(
        uri,
        arrayOf(
            OpenableColumns.DISPLAY_NAME
        ),
        null,
        null,
        null
    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            val index =
                cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

            if (index >= 0) {

                fileName =
                    cursor.getString(index)
            }
        }
    }

    return fileName
}

private data class ZipValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

private fun validateZipFile(
    context: Context,
    uri: Uri
): ZipValidationResult {

    return try {

        val inputStream =
            context.contentResolver
                .openInputStream(uri)
                ?: return ZipValidationResult(
                    false,
                    "Unable to open ZIP file."
                )

        ZipInputStream(
            BufferedInputStream(inputStream)
        ).use { zipInputStream ->

            var entry =
                zipInputStream.nextEntry

            var pdfFound = false

            while (entry != null) {

                if (!entry.isDirectory) {

                    val entryName =
                        entry.name

                    if (
                        !entryName
                            .lowercase()
                            .endsWith(".pdf")
                    ) {

                        return ZipValidationResult(

                            false,

                            "ZIP can contain only PDF files.\nInvalid file: $entryName"
                        )
                    }

                    pdfFound = true
                }

                zipInputStream.closeEntry()

                entry =
                    zipInputStream.nextEntry
            }

            // ZIP should contain at least one PDF

            if (!pdfFound) {

                return ZipValidationResult(
                    false,
                    "ZIP file does not contain any PDF files."
                )
            }
        }

        ZipValidationResult(
            isValid = true
        )

    } catch (e: Exception) {

        ZipValidationResult(

            false,

            "Unable to validate ZIP file."
        )
    }
}

private fun uriToFile(
    context: Context,
    uri: Uri
): File {

    val fileName =
        getFileName(context, uri)
            ?: "otdr_file"

    val tempFile =
        File(
            context.cacheDir,
            fileName
        )

    context.contentResolver
        .openInputStream(uri)
        ?.use { input ->

            tempFile.outputStream()
                .use { output ->
                    input.copyTo(output)
                }
        }

    return tempFile
}