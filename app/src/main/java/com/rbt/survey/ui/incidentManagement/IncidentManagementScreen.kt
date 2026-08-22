package com.rbt.survey.ui.incidentManagement

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rbt.survey.data.model.IncidentItem
import com.rbt.survey.data.model.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.google.android.gms.location.LocationServices
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.location.Location
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.annotation.SuppressLint
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentManagementScreen(
    viewModel: IncidentManagementViewModel,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val totalTicketCount by viewModel.totalTicketCount.collectAsState()
    val criticalCount by viewModel.criticalCount.collectAsState()
    val inProgressCount by viewModel.inProgressCount.collectAsState()
    val resolvedCount by viewModel.resolvedCount.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalRecords by viewModel.totalRecords.collectAsState()
    val pageSize by viewModel.pageSize.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedprojectassets by viewModel.selectedprojectassets.collectAsState()
    val isLoadingAssets by viewModel.isLoadingAssets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggingIncident by viewModel.isLoggingIncident.collectAsState()
    val incidentFilter by viewModel.filter.collectAsState()

    val totalPages = if (totalRecords == 0 || pageSize == 0) {
        0
    } else {
        (totalRecords + pageSize - 1) / pageSize
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showReportIncidentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadIncidentScreen()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Incident Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(horizontal = 16.dp)
        ) {
            
            // Report Incident
            Button(
                onClick = {
                    showReportIncidentDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(
                    vertical = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text("Report Incident")
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // SUMMARY CARDS

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {

                item {
                    IncidentSummaryCard(
                        title = "Total Tickets",
                        count = totalTicketCount,
                        icon = Icons.Default.ConfirmationNumber
                    )
                }
                item {
                    IncidentSummaryCard(
                        title = "Critical Priority",
                        count = criticalCount,
                        icon = Icons.Default.PriorityHigh
                    )
                }
                item {
                    IncidentSummaryCard(
                        title = "In Progress",
                        count = inProgressCount,
                        icon = Icons.Default.PendingActions
                    )
                }
                item {
                    IncidentSummaryCard(
                        title = "Resolved",
                        count = resolvedCount,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // INCIDENT TICKET HEADER

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Incident Ticket",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Sync
                IconButton(
                    onClick = {
                        viewModel.loadIncidentScreen()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SEARCH + FILTER

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchIncidents(it)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text("Search incident...")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.searchIncidents("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedButton(
                    onClick = {
                        showFilterDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {

                    items(
                        items = incidents,
                        key = { it.incidentId }
                    ) { incident ->

                        IncidentTicketCard(
                            incident = incident,
                            onClick = {
                                // TODO: Open incident details
                            }
                        )
                    }
                }

                IncidentPagination(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onPageSelected = { page ->
                        viewModel.loadIncidentPage(page)
                    }
                )
            }
        }
    }

    // FILTER DIALOG
    if (showFilterDialog) {

        IncidentFilterDialog(
            projects = projects,
            selectedprojectassets = selectedprojectassets,
            isLoadingAssets = isLoadingAssets,
            currentFilter = incidentFilter,
            onProjectSelected = { project ->

                if (project != null) {
                    viewModel.loadAssets(project.projectId)
                }
            },
            onDismiss = {
                showFilterDialog = false
            },
            onApply = { projectId,
                        status,
                        priority,
                        category,
                        assetId,
                        assigneeId ->

                viewModel.updateIncidentFilter(
                    projectId = projectId,
                    status = status,
                    priority = priority,
                    category = category,
                    assetId = assetId,
                    assigneeId = assigneeId
                )

                showFilterDialog = false

                viewModel.loadIncidentPage(1)
            },
            onClear = {
                viewModel.clearIncidentFilter()
            }
        )
    }

    if (showReportIncidentDialog) {

        ReportIncidentDialog(
            projects = projects,
            selectedprojectassets = selectedprojectassets,
            isLoadingAssets = isLoadingAssets,
            isLoggingIncident = isLoggingIncident,
            onProjectSelected = { project ->

                if (project != null) {
                    viewModel.loadAssets(project.projectId)
                }
            },

            onDismiss = {
                showReportIncidentDialog = false
            },

            onLogIncident = {
                    project,
                    asset,
                    title,
                    category,
                    priority,
                    description,
                    latitude,
                    longitude,
                    attachments ->

                viewModel.logIncident(
                    context = context,
                    projectId = project,
                    assetId = asset,
                    title = title,
                    category = category,
                    priority = priority,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    attachments = attachments,
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "Incident Logged Siccessfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        showReportIncidentDialog = false
                        viewModel.loadIncidentScreen()
                    },
                    onError = { errorMessage ->

                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        )
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
            }
        }
    }
}

@Composable
fun IncidentSummaryCard(
    title: String,
    count: Int,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .width(155.dp)
            .height(95.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2
                )
            }

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun IncidentTicketCard(
    incident: IncidentItem,
    onClick: () -> Unit
) {

    val formattedDate = try {

        val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        val outputFormatter = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy h:mm a"
        )

        LocalDateTime
            .parse(
                incident.createdOn,
                inputFormatter
            )
            .format(outputFormatter)

    } catch (e: Exception) {

        incident.createdOn
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = incident.incidentCode,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IncidentPriorityChip(
                    priority = incident.priority
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Project: ${incident.projectName}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Asset: ${incident.assetNameSnapshot ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IncidentStatusChip(
                    status = incident.status
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun IncidentPriorityChip(
    priority: String
) {
    AssistChip(
        onClick = {},
        label = {
            Text(priority)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
fun IncidentStatusChip(
    status: String
) {
    AssistChip(
        onClick = {},
        label = {
            Text(status)
        },
        leadingIcon = {
            Icon(
                imageVector = when (status.lowercase()) {
                    "resolved" -> Icons.Default.CheckCircle
                    "in progress" -> Icons.Default.PendingActions
                    else -> Icons.Default.Circle
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

@Composable
fun IncidentPagination(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {

    if (totalPages <= 1) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        // -----------------------------------------
        // PREVIOUS
        // -----------------------------------------

        IconButton(
            onClick = {
                if (currentPage > 1) {
                    onPageSelected(currentPage - 1)
                }
            },
            enabled = currentPage > 1
        ) {
            Text(
                text = "‹",
                fontSize = 28.sp
            )
        }


        // -----------------------------------------
        // PAGE NUMBERS
        // -----------------------------------------

        val pages = remember(currentPage, totalPages) {

            val result = mutableListOf<Int?>()

            if (totalPages <= 5) {

                for (page in 1..totalPages) {
                    result.add(page)
                }

            } else {

                // First page
                result.add(1)

                if (currentPage > 3) {
                    result.add(null) // ...
                }

                val startPage = maxOf(2, currentPage - 1)
                val endPage = minOf(totalPages - 1, currentPage + 1)

                for (page in startPage..endPage) {
                    result.add(page)
                }

                if (currentPage < totalPages - 2) {
                    result.add(null) // ...
                }

                // Last page
                result.add(totalPages)
            }

            result
        }


        // -----------------------------------------
        // DISPLAY PAGES
        // -----------------------------------------

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            pages.forEach { page ->

                if (page == null) {

                    Text(
                        text = "...",
                        modifier = Modifier.padding(
                            horizontal = 6.dp
                        ),
                        fontSize = 16.sp
                    )

                } else {

                    if (page == currentPage) {

                        Button(
                            onClick = {},
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = page.toString()
                            )
                        }

                    } else {

                        OutlinedButton(
                            onClick = {
                                onPageSelected(page)
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = page.toString()
                            )
                        }
                    }
                }
            }
        }


        // -----------------------------------------
        // NEXT
        // -----------------------------------------

        IconButton(
            onClick = {
                if (currentPage < totalPages) {
                    onPageSelected(currentPage + 1)
                }
            },
            enabled = currentPage < totalPages
        ) {
            Text(
                text = "›",
                fontSize = 28.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentDialog(
    projects: List<ProjectResponse>,
    selectedprojectassets: List<CreatedAssetData>,
    isLoadingAssets: Boolean,
    onProjectSelected: (ProjectResponse?) -> Unit,
    isLoggingIncident: Boolean,
    onDismiss: () -> Unit,
    onLogIncident: (
        Long,
        Long?,
        String,
        String,
        String,
        String,
        Double,
        Double,
        List<Uri>?
    ) -> Unit
) {

    val context = LocalContext.current

    var selectedProject by remember {
        mutableStateOf<ProjectResponse?>(null)
    }

    var selectedAsset by remember {
        mutableStateOf<CreatedAssetData?>(null)
    }

    var incidentTitle by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("")
    }

    var selectedPriority by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var latitude by remember {
        mutableStateOf<Double?>(null)
    }

    var longitude by remember {
        mutableStateOf<Double?>(null)
    }

    var isGettingLocation by remember {
        mutableStateOf(false)
    }

    var locationError by remember {
        mutableStateOf<String?>(null)
    }

    var attachments by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    val attachmentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            if (uris.isNotEmpty()) {

                attachments =
                    (attachments + uris).distinct()
            }
        }

    var showValidation by remember {
        mutableStateOf(false)
    }

    val categoryOptions = listOf(
        "FIBER_CUT",
        "DEVICE_DOWN",
        "POWER_ISSUE",
        "OTHER"
    )

    val priorityOptions = listOf(
        "CRITICAL",
        "HIGH",
        "MEDIUM",
        "LOW"
    )

    // ---------------------------------------------------------
    // FILE PICKER - MULTIPLE IMAGE/PDF
    // ---------------------------------------------------------

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            if (uris.isNotEmpty()) {
                attachments =
                    attachments + uris
            }
        }

    // ---------------------------------------------------------
    // VALIDATION
    // ---------------------------------------------------------

    val isValid =
        selectedProject != null &&
                incidentTitle.isNotBlank() &&
                selectedCategory.isNotBlank() &&
                selectedPriority.isNotBlank() &&
                description.isNotBlank() &&
                latitude != null &&
                longitude != null

    Dialog(
        onDismissRequest = {

            if (!isLoggingIncident) {
                onDismiss()
            }
        }
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // =================================================
                    // HEADER
                    // =================================================

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                top = 16.dp,
                                end = 10.dp,
                                bottom = 14.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Report Incident",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onDismiss
                        ) {

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    HorizontalDivider()

                    // =================================================
                    // SCROLLABLE CONTENT
                    // =================================================

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 16.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp)
                    ) {

                        // -------------------------------------------------
                        // PROJECT
                        // -------------------------------------------------

                        item {

                            IncidentProjectDropdown(
                                projects = projects,
                                selectedProject = selectedProject,
                                required = true,
                                showError = showValidation && selectedProject == null,
                                onSelected = { project ->

                                    selectedProject = project

                                    selectedAsset = null

                                    onProjectSelected(project)
                                }
                            )
                        }

                        // -------------------------------------------------
                        // ASSET
                        // -------------------------------------------------

                        item {

                            ReportIncidentAssetDropdown(
                                selectedProject = selectedProject,
                                selectedAsset = selectedAsset,
                                assets = selectedprojectassets,
                                isLoadingAssets = isLoadingAssets,
                                onSelected = { asset ->

                                    selectedAsset = asset
                                }
                            )
                        }

                        // -------------------------------------------------
                        // TITLE
                        // -------------------------------------------------

                        item {

                            ReportIncidentTextField(
                                label = "Incident Title",
                                value = incidentTitle,
                                onValueChange = {
                                    incidentTitle = it
                                },
                                placeholder = "Enter incident title",
                                required = true,
                                showError =
                                    showValidation &&
                                            incidentTitle.isBlank()
                            )
                        }

                        // -------------------------------------------------
                        // CATEGORY
                        // -------------------------------------------------

                        item {

                            ReportIncidentSimpleDropdown(
                                label = "Category",
                                selectedValue =
                                    if (selectedCategory.isBlank()) {
                                        "Select Category"
                                    } else {
                                        selectedCategory
                                    },
                                options = categoryOptions,
                                required = true,
                                showError =
                                    showValidation &&
                                            selectedCategory.isBlank(),
                                onSelected = {
                                    selectedCategory = it
                                }
                            )
                        }

                        // -------------------------------------------------
                        // PRIORITY
                        // -------------------------------------------------

                        item {

                            ReportIncidentSimpleDropdown(
                                label = "Priority",
                                selectedValue =
                                    if (selectedPriority.isBlank()) {
                                        "Select Priority"
                                    } else {
                                        selectedPriority
                                    },
                                options = priorityOptions,
                                required = true,
                                showError =
                                    showValidation &&
                                            selectedPriority.isBlank(),
                                onSelected = {
                                    selectedPriority = it
                                }
                            )
                        }

                        // -------------------------------------------------
                        // DESCRIPTION
                        // -------------------------------------------------

                        item {

                            ReportIncidentTextField(
                                label = "Description",
                                value = description,
                                onValueChange = {
                                    description = it
                                },
                                placeholder =
                                    "Enter incident description",
                                required = true,
                                showError =
                                    showValidation &&
                                            description.isBlank(),
                                minLines = 4
                            )
                        }

                        // -------------------------------------------------
                        // LOCATION
                        // -------------------------------------------------

                        item {

                            ReportIncidentLocationSection(
                                latitude = latitude,
                                longitude = longitude,
                                isGettingLocation = isGettingLocation,
                                locationError = locationError,
                                required = true,
                                showError = showValidation &&
                                        (latitude == null || longitude == null),

                                onGetLocation = {

                                    locationError = null
                                    isGettingLocation = true

                                    getCurrentLocation(
                                        context = context,

                                        onLocationReceived = { lat, lon ->

                                            latitude = lat
                                            longitude = lon

                                            isGettingLocation = false
                                            locationError = null
                                        },

                                        onError = { error ->

                                            isGettingLocation = false
                                            locationError = error
                                        }
                                    )
                                }
                            )
                        }

                        // -------------------------------------------------
                        // ATTACHMENTS
                        // -------------------------------------------------

                        item {

                            ReportIncidentAttachmentSection(
                                attachments = attachments,
                                onAddAttachment = {

                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "image/*",
                                            "application/pdf"
                                        )
                                    )
                                },
                                onRemoveAttachment = { uri ->

                                    attachments =
                                        attachments.filter {
                                            it != uri
                                        }
                                }
                            )
                        }
                    }

                    // =================================================
                    // FIXED BOTTOM BUTTONS
                    // =================================================

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isLoggingIncident,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {

                            Text("Cancel")
                        }

                        Button(
                            onClick = {

                                showValidation = true

                                if (isValid) {

                                    onLogIncident(
                                        selectedProject!!.projectId.toLong(),
                                        selectedAsset?.assetId?.toLong(),
                                        incidentTitle.trim(),
                                        selectedCategory,
                                        selectedPriority,
                                        description.trim(),
                                        latitude!!,
                                        longitude!!,
                                        attachments.takeIf { it.isNotEmpty() }
                                    )
                                }
                            },
                            enabled = !isLoggingIncident,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {

                            Text("Log Incident")

                        }
                    }
                }
                if (isLoggingIncident) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ),
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
                                    text = "Logging Incident..."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentAssetDropdown(
    selectedProject: ProjectResponse?,
    selectedAsset: CreatedAssetData?,
    assets: List<CreatedAssetData>,
    isLoadingAssets: Boolean,
    onSelected: (CreatedAssetData?) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    val filteredAssets = remember(
        assets,
        searchQuery
    ) {
        if (searchQuery.isBlank()) {
            assets
        } else {

            assets.filter { asset ->

                val assetName =
                    asset.data
                        ?.get("assetName")
                        ?.asString
                        ?: ""

                asset.assetId
                    .toString()
                    .contains(
                        searchQuery,
                        ignoreCase = true
                    ) ||
                        assetName.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        asset.assetCode.contains(
                            searchQuery,
                            ignoreCase = true
                        )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Asset",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // ----------------------------------------------------
        // SELECTED ASSET FIELD
        // ----------------------------------------------------

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            val assetDisplayName =
                selectedAsset?.let { asset ->

                    val assetName =
                        asset.data
                            ?.get("assetName")
                            ?.asString
                            ?: ""

                    "$assetName (${asset.assetId})"

                } ?: "All Assets"

            OutlinedTextField(
                value = when {

                    selectedProject == null ->
                        "Select Project First"

                    isLoadingAssets ->
                        "Loading Assets..."

                    else ->
                        assetDisplayName
                },
                onValueChange = {},
                readOnly = true,
                enabled =
                    selectedProject != null &&
                            !isLoadingAssets,
                modifier = Modifier.fillMaxWidth(),
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
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor =
                        MaterialTheme.colorScheme.onSurface,

                    disabledBorderColor =
                        MaterialTheme.colorScheme.outline,

                    disabledLabelColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    disabledTrailingIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        enabled =
                            selectedProject != null &&
                                    !isLoadingAssets
                    ) {
                        expanded = !expanded
                    }
            )
        }

        // ----------------------------------------------------
        // DROPDOWN
        // ----------------------------------------------------

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

                    // ------------------------------------------------
                    // SEARCH
                    // ------------------------------------------------

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
                            Text("Search asset...")
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

                    // ------------------------------------------------
                    // ALL ASSETS
                    // ------------------------------------------------

                    DropdownMenuItem(
                        text = {
                            Text("All Assets")
                        },
                        trailingIcon = {

                            if (selectedAsset == null) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        },
                        onClick = {

                            onSelected(null)

                            searchQuery = ""
                            expanded = false
                        }
                    )

                    HorizontalDivider()

                    // ------------------------------------------------
                    // ASSET LIST
                    // ------------------------------------------------

                    if (filteredAssets.isEmpty()) {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    if (searchQuery.isBlank()) {
                                        "No assets available"
                                    } else {
                                        "No matching assets"
                                    }
                            )
                        }

                    } else {

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
                                items = filteredAssets,
                                key = {
                                    it.assetId
                                }
                            ) { asset ->

                                val assetName =
                                    asset.data
                                        ?.get("assetName")
                                        ?.asString
                                        ?: ""

                                DropdownMenuItem(
                                    text = {

                                        Column(
                                            verticalArrangement =
                                                Arrangement.spacedBy(
                                                    4.dp
                                                )
                                        ) {

                                            Text(
                                                text =
                                                    "ID: ${asset.assetId}",
                                                fontWeight =
                                                    FontWeight.SemiBold
                                            )

                                            Text(
                                                text =
                                                    "Name: $assetName"
                                            )

                                            Text(
                                                text =
                                                    "Code: ${asset.assetCode}",
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            )
                                        }
                                    },

                                    trailingIcon = {

                                        if (
                                            selectedAsset?.assetId ==
                                            asset.assetId
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

                                        onSelected(asset)

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
fun ReportIncidentTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    required: Boolean = false,
    showError: Boolean = false,
    minLines: Int = 1
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text =
                if (required) {
                    "$label *"
                } else {
                    label
                },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder)
            },
            minLines = minLines,
            maxLines = if (minLines > 1) 6 else 1,
            isError = showError,
            shape = RoundedCornerShape(10.dp),
            singleLine = minLines == 1
        )

        if (showError) {

            Text(
                text = "$label is required",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 4.dp
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentSimpleDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    required: Boolean = false,
    showError: Boolean = false,
    onSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text =
                if (required) {
                    "$label *"
                } else {
                    label
                },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {

            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {

                    ExposedDropdownMenuDefaults
                        .TrailingIcon(
                            expanded = expanded
                        )
                },
                isError = showError,
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(option)
                        },
                        onClick = {

                            onSelected(option)

                            expanded = false
                        }
                    )
                }
            }
        }

        if (showError) {

            Text(
                text = "$label is required",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 4.dp
                )
            )
        }
    }
}

@Composable
fun ReportIncidentLocationSection(
    latitude: Double?,
    longitude: Double?,
    isGettingLocation: Boolean,
    locationError: String?,
    required: Boolean,
    showError: Boolean,
    onGetLocation: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = if (required) {
                "Location *"
            } else {
                "Location"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {

                if (
                    latitude != null &&
                    longitude != null
                ) {

                    // -----------------------------------------
                    // LOCATION FOUND
                    // -----------------------------------------

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.LocationOn,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text = "Location captured",
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Latitude: ${
                                        String.format(
                                            "%.6f",
                                            latitude
                                        )
                                    }",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )

                            Text(
                                text =
                                    "Longitude: ${
                                        String.format(
                                            "%.6f",
                                            longitude
                                        )
                                    }",
                                style =
                                    MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    OutlinedButton(
                        onClick = onGetLocation,
                        modifier =
                            Modifier.fillMaxWidth(),
                        enabled = !isGettingLocation,
                        shape =
                            RoundedCornerShape(10.dp)
                    ) {

                        if (isGettingLocation) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text("Getting Location...")

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Default.LocationOn,
                                contentDescription =
                                    null
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(6.dp)
                            )

                            Text("Update Location")
                        }
                    }

                } else {

                    // -----------------------------------------
                    // NO LOCATION
                    // -----------------------------------------

                    Text(
                        text = "Location not captured",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Button(
                        onClick = onGetLocation,
                        modifier =
                            Modifier.fillMaxWidth(),
                        enabled = !isGettingLocation,
                        shape =
                            RoundedCornerShape(10.dp)
                    ) {

                        if (isGettingLocation) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text("Getting Location...")

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Default.LocationOn,
                                contentDescription =
                                    null
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(6.dp)
                            )

                            Text("Get Location")
                        }
                    }
                }

                // -----------------------------------------
                // LOCATION ERROR
                // -----------------------------------------

                if (locationError != null) {

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = locationError,
                        color =
                            MaterialTheme.colorScheme.error,
                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (
            showError &&
            latitude == null &&
            longitude == null &&
            locationError == null
        ) {

            Text(
                text = "Location is required",
                color =
                    MaterialTheme.colorScheme.error,
                style =
                    MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier.padding(
                        start = 4.dp,
                        top = 4.dp
                    )
            )
        }
    }
}

@Composable
fun ReportIncidentAttachmentSection(
    attachments: List<Uri>,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (Uri) -> Unit
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Attachments",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedButton(
            onClick = onAddAttachment,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {

            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Text("Add Attachments")
        }

        if (attachments.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                attachments.forEach { uri ->

                    val fileName = remember(uri) {
                        getFileName(context, uri)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    if (
                                        fileName
                                            .lowercase()
                                            .endsWith(".pdf")
                                    ) {
                                        Icons.Default.PictureAsPdf
                                    } else {
                                        Icons.Default.Image
                                    },
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.width(10.dp)
                            )

                            Text(
                                text = fileName,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(
                                onClick = {
                                    onRemoveAttachment(uri)
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Close,
                                    contentDescription =
                                        "Remove attachment"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFilterDialog(
    projects: List<ProjectResponse>,
    selectedprojectassets: List<CreatedAssetData>,
    currentFilter: IncidentFilter,
    isLoadingAssets: Boolean,
    onProjectSelected: (ProjectResponse?) -> Unit,
    onDismiss: () -> Unit,
    onApply: (projectId: Long?,
              status: String?,
              priority: String?,
              category: String?,
              assetId: Long?,
              assigneeId: Long?
    ) -> Unit,
    onClear: () -> Unit
) {

    var selectedProject by remember(currentFilter) {
        mutableStateOf(
            projects.firstOrNull {
                it.projectId.toLong() == currentFilter.projectId
            }
        )
    }

    var selectedStatus by remember(currentFilter) {
        mutableStateOf(
            currentFilter.status ?: "All Statuses"
        )
    }

    var selectedPriority by remember(currentFilter) {
        mutableStateOf(
            currentFilter.priority ?: "All Priorities"
        )
    }

    var selectedCategory by remember(currentFilter) {
        mutableStateOf(
            currentFilter.category ?: "All Categories"
        )
    }

    var selectedAssetId by remember(currentFilter) {
        mutableStateOf(
            currentFilter.assetId?.toInt()
        )
    }

    var selectedAsset = remember(
        selectedAssetId,
        selectedprojectassets
    ) {
        selectedprojectassets
            .firstOrNull {
                it.assetId == selectedAssetId
            }
            ?.assetCode
            ?: if (selectedAssetId == null) {
                "All Assets"
            } else {
                "Asset #$selectedAssetId"
            }
    }
    var selectedAssignee by remember { mutableStateOf("All Assignees") }

    val statusOptions = listOf(
        "All Statuses",
        "NEW",
        "ASSIGNED",
        "IN_PROGRESS",
        "RESOLVED",
        "CLOSED",
        "CANCELLED"
    )

    val priorityOptions = listOf(
        "All Priorities",
        "CRITICAL",
        "HIGH",
        "MEDIUM",
        "LOW"
    )

    val categoryOptions = listOf(
        "All Categories",
        "FIBER_CUT",
        "DEVICE_DOWN",
        "POWER_ISSUE",
        "OTHER"
    )

    Dialog(
        onDismissRequest = onDismiss
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // HEADER

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            top = 16.dp,
                            end = 12.dp,
                            bottom = 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Filter Incidents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider()

                // FILTERS

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(20.dp)
                ) {

                    IncidentProjectDropdown(
                        projects = projects,
                        required = false,
                        showError = false,
                        selectedProject = selectedProject,
                        onSelected = { project ->

                            selectedProject = project

                            selectedAsset = "All Assets"
                            selectedAssetId = null
                            selectedAssignee = "All Assignees"

                            onProjectSelected(project)
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    IncidentFilterDropdown(
                        label = "Status",
                        selectedValue = selectedStatus,
                        options = statusOptions,
                        onSelected = {
                            selectedStatus = it
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    IncidentFilterDropdown(
                        label = "Priority",
                        selectedValue = selectedPriority,
                        options = priorityOptions,
                        onSelected = {
                            selectedPriority = it
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    IncidentFilterDropdown(
                        label = "Category",
                        selectedValue = selectedCategory,
                        options = categoryOptions,
                        onSelected = {
                            selectedCategory = it
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    IncidentAssetDropdown(
                        selectedProject = selectedProject,
                        selectedAsset = selectedAsset,
                        selectedAssetId = selectedAssetId,
                        assets = selectedprojectassets,
                        isLoadingAssets = isLoadingAssets,
                        onSelected = { assetId, assetCode ->
                            selectedAssetId = if (assetId == -1) null else assetId
                            selectedAsset = assetCode
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    IncidentFilterDropdown(
                        label = "Assignee",
                        selectedValue = if (selectedProject == null) {
                            "Select Project First"
                        } else {
                            selectedAssignee
                        },
                        options = listOf("All Assignees"),
                        enabled = selectedProject != null,
                        onSelected = {
                            selectedAssignee = it
                        }
                    )
                }

                HorizontalDivider()

                // -----------------------------------------
                // BUTTONS
                // -----------------------------------------

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedButton(
                        onClick = {

                            selectedProject = null
                            selectedStatus = "All Statuses"
                            selectedPriority = "All Priorities"
                            selectedCategory = "All Categories"
                            selectedAsset = "All Assets"
                            selectedAssetId = null
                            selectedAssignee = "All Assignees"

                            onClear()

                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear All")
                    }

                    Button(
                        onClick = {
                            onApply(
                                selectedProject?.projectId?.toLong(),

                                selectedStatus
                                    .takeIf { it != "All Statuses" },

                                selectedPriority
                                    .takeIf { it != "All Priorities" },

                                selectedCategory
                                    .takeIf { it != "All Categories" },

                                selectedAssetId
                                    ?.takeIf { it != -1 }
                                    ?.toLong(),

                                // For now your Assignee dropdown only has All Assignees
                                null
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentProjectDropdown(
    projects: List<ProjectResponse>,
    selectedProject: ProjectResponse?,
    required: Boolean = false,
    showError: Boolean = false,
    onSelected: (ProjectResponse?) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Project",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {

            OutlinedTextField(
                value = selectedProject?.projectName ?: "All Projects",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                // -----------------------------------------
                // ALL PROJECTS
                // -----------------------------------------

                DropdownMenuItem(
                    text = {
                        Text("All Projects")
                    },
                    onClick = {

                        onSelected(null)

                        expanded = false
                    }
                )

                // -----------------------------------------
                // PROJECTS
                // -----------------------------------------

                projects.forEach { project ->

                    DropdownMenuItem(
                        text = {
                            Text(project.projectName)
                        },
                        onClick = {

                            onSelected(project)

                            expanded = false
                        }
                    )
                }
            }
        }
        if (showError) {

            Text(
                text = "Project is required",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 4.dp
                )
            )
        }
    }
}

@Composable
fun IncidentAssetDropdown(
    selectedProject: ProjectResponse?,
    selectedAsset: String,
    selectedAssetId: Int?,
    assets: List<CreatedAssetData>,
    isLoadingAssets: Boolean,
    onSelected: (Int, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var textFieldSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    var textFieldPosition by remember {
        mutableStateOf(IntOffset.Zero)
    }

    val filteredAssets = remember(
        assets,
        searchQuery
    ) {
        if (searchQuery.isBlank()) {
            assets
        } else {
            assets.filter { asset ->

                val assetName = asset.data?.get("assetName")?.asString ?: ""

                asset.assetId.toString().contains(
                    searchQuery,
                    ignoreCase = true
                ) ||
                        assetName.contains(
                            searchQuery,
                            ignoreCase = true
                        ) ||
                        asset.assetCode.contains(
                            searchQuery,
                            ignoreCase = true
                        )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Asset",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedTextField(
                value = when {

                    selectedProject == null ->
                        "Select Project First"

                    isLoadingAssets ->
                        "Loading Assets..."

                    else ->
                        selectedAsset
                },
                onValueChange = {},
                readOnly = true,
                enabled = selectedProject != null && !isLoadingAssets,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->

                        textFieldSize = coordinates.size

                        textFieldPosition =
                            coordinates.positionInWindow()
                                .let {
                                    IntOffset(
                                        it.x.toInt(),
                                        it.y.toInt()
                                    )
                                }
                    },
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
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor =
                        MaterialTheme.colorScheme.outline,
                    disabledLabelColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        enabled =
                            selectedProject != null &&
                                    !isLoadingAssets
                    ) {
                        expanded = !expanded
                    }
            )
        }

        if (expanded) {

            AnimatedVisibility(
                visible = expanded
            ) {

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
                                Text("Search asset...")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
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
                                            Icons.Default.Clear,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Text("All Assets")
                            },
                            trailingIcon = {

                                if (selectedAsset == "All Assets") {

                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {

                                onSelected(
                                    -1,
                                    "All Assets"
                                )

                                searchQuery = ""

                                expanded = false
                            }
                        )

                        HorizontalDivider()

                        if (filteredAssets.isEmpty()) {

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    if (searchQuery.isBlank()) {
                                        "No assets available"
                                    } else {
                                        "No matching assets"
                                    }
                                )
                            }
                        } else {

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(8.dp)
                            ) {

                                items(
                                    items = filteredAssets,
                                    key = { it.assetId }
                                ) { asset ->

                                    val assetName =
                                        asset.data?.get("assetName")?.asString ?: ""

                                    DropdownMenuItem(
                                        text = {

                                            Column(
                                                verticalArrangement =
                                                    Arrangement.spacedBy(4.dp)
                                            ) {

                                                Text(
                                                    text = "ID: ${asset.assetId}",
                                                    fontWeight =
                                                        FontWeight.SemiBold
                                                )

                                                Text(
                                                    text = "Name: $assetName"
                                                )

                                                Text(
                                                    text = "Code: ${asset.assetCode}",
                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurfaceVariant
                                                )
                                            }
                                        },
                                        trailingIcon = {

                                            if (selectedAssetId == asset.assetId) {

                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        onClick = {

                                            onSelected(
                                                asset.assetId,
                                                asset.assetCode
                                            )

                                            searchQuery = ""

                                            expanded = false
                                        }
                                    )

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                bottom = 6.dp
            )
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (enabled) {
                    expanded = !expanded
                }
            }
        ) {

            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(option)
                        },
                        onClick = {

                            onSelected(option)

                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(
    context: Context,
    onLocationReceived: (Double, Double) -> Unit,
    onError: (String) -> Unit
) {

    val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    val cancellationTokenSource =
        CancellationTokenSource()

    fusedLocationClient
        .getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )
        .addOnSuccessListener { location: Location? ->

            if (location != null) {

                onLocationReceived(
                    location.latitude,
                    location.longitude
                )

            } else {

                onError("Unable to get current location")
            }
        }
        .addOnFailureListener { exception ->

            onError(
                exception.message
                    ?: "Failed to get location"
            )
        }
}

fun getFileName(
    context: Context,
    uri: Uri
): String {

    var fileName = "Unknown file"

    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            val nameIndex =
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }

    return fileName
}