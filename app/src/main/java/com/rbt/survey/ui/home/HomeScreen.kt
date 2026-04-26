package com.rbt.survey.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.rbt.survey.data.model.FormData
import com.rbt.survey.data.model.BlockSummary
import com.rbt.survey.data.model.GpItem
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.text.font.FontWeight
import com.rbt.survey.data.model.Assignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import com.rbt.survey.data.model.SubmissionItem
import com.rbt.survey.data.local.db.OfflineSubmission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToMap: (Int, String?) -> Unit,
    onNavigateToEditOfflineSubmission: (Int, Int, String?, String?) -> Unit,
    onLogout: () -> Unit,
    onNavigateToDgpsSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val blockSummaries by viewModel.blockSummaries.collectAsState()
    val blockSummaryLoading by viewModel.blockSummaryLoading.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()

//    var selectedForm by remember { mutableStateOf<FormData?>(null) }
    val selectedForm by viewModel.selectedForm.collectAsState()
//    var expandedFormMenu by remember { mutableStateOf(false) }
    var expandedAssignedMenu by remember { mutableStateOf(false) }
    var expandedUploadedMenu by remember { mutableStateOf(false) }

    val tabs = listOf("Assigned", "Completed", "Submitted", "Uploaded")

    val icons = listOf(
        Icons.Default.Assignment,
        Icons.Default.CheckCircle,
        Icons.Default.CloudDone,
        Icons.Default.CloudUpload
    )

    Scaffold(
        topBar = {
            Column {
                // 🔹 TOP BAR
                CenterAlignedTopAppBar(
                    title = { Text("Survey Dashboard", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { viewModel.fetchForms() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = {
                                    showMenu = false
                                    viewModel.logout()
                                    onLogout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Logout, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("DGPS & CORS") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToDgpsSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                        }
                    }
                )

                val forms = (uiState as? HomeUiState.Success)?.forms
                    ?.filter { it.isActive } ?: emptyList()

                var expandedFormMenu by remember { mutableStateOf(false) }

                if (forms.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expandedFormMenu,
                            onExpandedChange = { expandedFormMenu = !expandedFormMenu }
                        ) {
                            OutlinedTextField(
                                value = selectedForm?.formName ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Form") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFormMenu)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = expandedFormMenu,
                                onDismissRequest = { expandedFormMenu = false }
                            ) {
                                forms.forEach { form ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    form.formName,
                                                    fontWeight = FontWeight.Medium
                                                )

                                                Text(
                                                    form.description ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectForm(form)
                                            expandedFormMenu = false

                                            // 🔥 IMPORTANT: CALL API BASED ON TAB
                                            when (selectedTabIndex) {
                                                0 -> viewModel.fetchBlockSummary(form.formId)
                                                1 -> viewModel.fetchCompletedBlockSummary(form.formId)
                                                2 -> viewModel.fetchOfflineSubmissions(form.formId)
                                                3 -> viewModel.fetchUploadedSubmissions(form.formId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 🔹 TABS
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                viewModel.setSelectedTabIndex(index)

                                selectedForm?.let { form ->
                                    when (index) {
                                        0 -> viewModel.fetchBlockSummary(form.formId)
                                        1 -> viewModel.fetchCompletedBlockSummary(form.formId)
                                        2 -> viewModel.fetchOfflineSubmissions(form.formId)
                                        3 -> viewModel.fetchUploadedSubmissions(form.formId)
                                    }
                                }
                            },
                            text = { Text(title) },
                            icon = { Icon(icons[index], contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 🔹 DROPDOWN
            when (selectedTabIndex) {

                // 🔹 Assigned
                0 -> {

//                    val forms = (uiState as? HomeUiState.Success)?.forms
//                        ?.filter { it.isActive } ?: emptyList()

                    if (selectedForm == null) {
                        EmptyState("Please select a form")
                    } else {

                        // 🔥 ONLY BLOCK TILES (NO HEADER)
                        if (blockSummaryLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (blockSummaries.isEmpty()) {
                            EmptyState("No block summary data available")
                        } else {
                            val filteredBlocks = blockSummaries.filter { it.completedPercentage < 100 }

                            if (filteredBlocks.isEmpty()) {
                                EmptyState("All blocks are completed 🎉")
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(filteredBlocks) { summary ->
                                        BlockSummaryCard(
                                            summary = summary,
                                            onClick = { blockCode ->
                                                viewModel.setSelectedGpStatusList(summary.gpList)
                                                selectedForm?.let {
                                                    onNavigateToMap(it.formId, blockCode)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // 🔹 Completed
                1 -> {

                    if (selectedForm == null) {
                        EmptyState("Please select a form")
                    } else {
                        val blockSummaries by viewModel.completedBlockSummaries.collectAsState()
                        val loading by viewModel.completedLoading.collectAsState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // 🔹 Loading
                            if (loading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            // 🔹 Filter completed blocks
                            else {
                                val completedBlocks = blockSummaries.filter {
                                    it.completedPercentage == 100
                                }

                                if (completedBlocks.isEmpty()) {
                                    EmptyState("No completed blocks")
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(completedBlocks) { summary ->
                                            BlockSummaryCard(
                                                summary = summary,
                                                onClick = { blockCode ->
                                                    viewModel.setSelectedGpStatusList(summary.gpList)
                                                    selectedForm?.let {
                                                        onNavigateToMap(it.formId, blockCode)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 🔹 Submitted
                2 -> {
                    if (selectedForm == null) {
                        EmptyState("Please select a form")
                    } else {
                        val offlineItems by viewModel.offlineSubmissions.collectAsState()
                        val loading by viewModel.offlineLoading.collectAsState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            if (loading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (offlineItems.isEmpty()) {
                                EmptyState("No submitted data offline")
                            } else {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(offlineItems) { item ->
                                        OfflineSubmittedTile(
                                            item = item,
                                            onClick = {
                                                selectedForm?.let { form ->
                                                    onNavigateToEditOfflineSubmission(
                                                        form.formId,
                                                        item.id,
                                                        item.blockCode,
                                                        item.gp
                                                    )
                                                }
                                            },
                                            onUploadClick = {
                                                viewModel.uploadSubmission(context, item.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 🔹 Uploaded
                3 -> {

                    if (selectedForm == null) {
                        EmptyState("Please select a form")
                    } else {
                        val uploadedItems by viewModel.uploadedItems.collectAsState()
                        val loading by viewModel.uploadedLoading.collectAsState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // 🔹 LOADING
                            if (loading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            // 🔹 EMPTY
                            else if (uploadedItems.isEmpty()) {
                                EmptyState("No uploaded data")
                            }

                            // 🔹 LIST
                            else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(uploadedItems) { item ->
                                        UploadedTile(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }

//            if (selectedTabIndex == 0) {
//            }
        }
    }
}

@Composable
fun UploadedTile(item: SubmissionItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // 🔹 ID
            Text(
                text = "ID : ${item.submissionId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 🔹 GP
            Text(
                text = "GP : ${item.data.GP ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )

            // 🔹 Block
            Text(
                text = "Block : ${item.data.Block ?: "-"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 🔹 Created On
            Text(
                text = "Submitted On : ${formatDateTime(item.createdOn)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 🔹 Updated On (only if not null)
            if (!item.updatedOn.isNullOrEmpty()) {
                Text(
                    text = "Updated On : ${formatDateTime(item.updatedOn)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatDateTime(input: String?): String {
    return try {
        if (input.isNullOrEmpty()) return "-"

        val parts = input.split("T")

        val date = parts[0] // 2026-04-20
        val time = parts.getOrNull(1)?.take(5) ?: "" // 15:03

        val dateParts = date.split("-")
        val formattedDate = if (dateParts.size >= 3) {
            "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"
        } else {
            date
        }

        "$formattedDate $time"
    } catch (e: Exception) {
        "-"
    }
}

@Composable
fun OfflineSubmittedTile(item: OfflineSubmission, onClick: () -> Unit, onUploadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ID : ${item.id} (Offline)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "GP : ${item.gp ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Block : ${item.blockCode ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                val format = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                val dateString = format.format(Date(item.timestamp))

                Text(
                    text = "Saved On : $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(onClick = onUploadClick) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload")
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun FormList(
    forms: List<FormData>,
    blockCode: String?,
    onFormClick: (Int) -> Unit,
    onFormLongClick: (FormData) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(forms.size) { index ->
            FormCard(forms[index], blockCode, onFormClick, onFormLongClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormCard(
    form: FormData,
    blockCode: String?,
    onFormClick: (Int) -> Unit,
    onFormLongClick: (FormData) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFormClick(form.formId) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = form.formName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = if (form.isActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(
                        text = if (form.isActive) "Active" else "Inactive",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (form.isActive)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (!form.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = form.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(label = "Code", value = form.formCode)
                InfoChip(label = "Version", value = "v${form.currentVersionNo}")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Block Summary button
            OutlinedButton(
                onClick = { onFormLongClick(form) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Summarize,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("View Block Summary")
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
fun BlockSummaryCard(
    summary: BlockSummary,
    onClick: (String) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(summary.blockCode) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 🔹 Block header (NO CLICK, NO EXPAND)
            Column {
                Text(
                    text = summary.blockName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Code: ${summary.blockCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatChip(
                    label = "Total GP",
                    value = "${summary.totalGP}",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatChip(
                    label = "Completed",
                    value = "${summary.completedPercentage}%",
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    textColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                StatChip(
                    label = "Pending",
                    value = "${summary.pendingPercentage}%",
                    color = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Progress bar
            LinearProgressIndicator(
                progress = { summary.completedPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun GpItemRow(gp: GpItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (gp.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (gp.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = gp.gpName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (gp.isCompleted) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = gp.lgdCode,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}
