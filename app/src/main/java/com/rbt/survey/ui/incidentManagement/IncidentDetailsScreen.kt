package com.rbt.survey.ui.incidentManagement

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailsScreen(
    viewModel: IncidentDetailsViewModel,
    onBack: () -> Unit,
    incidentId: Int,
) {

    val context = LocalContext.current
    val BASE_URL = "https://webgis.rbt-ltd.com/"
    val incidentDetails by viewModel.incidentDetails.collectAsState()
    val impactAssets by viewModel.impactAssets.collectAsState()
    val isPostingComment by viewModel.isPostingComment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showCommentsDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadIncidentDetails(incidentId)
    }

    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Incident Detail", fontWeight = FontWeight.Bold) },
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

        if (incidentDetails == null) {

            // Loading / empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("No incident details found")
                }
            }

        } else {

            val affectedPorts = incidentDetails!!.incident.affectedPorts ?: emptyList()
            val affectedCores = incidentDetails!!.incident.affectedCores ?: emptyList()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    item {

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {

                            item {
                                Button(
                                    onClick = {
                                        viewModel.DownloadIncidentReport(
                                            incidentId = incidentDetails!!.incident.incidentId,
                                            context = context
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text("Download Report")
                                }
                            }

                            item {
                                OutlinedButton(
                                    onClick = {
                                        showCommentsDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Comment,
                                        contentDescription = null
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text("Comments (${incidentDetails?.comments?.size ?: 0})")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Heading

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                incidentDetails!!.incident.title?.let {
                                    Text(
                                        text = it,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(incidentDetails!!.incident.incidentCode)
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    ) {

                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            incidentDetails!!.incident.status?.let { Text(it) }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            incidentDetails!!.incident.priority?.let { Text(it) }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SLA DETAILS

                        Text(
                            text = "SLA DETAILS",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = "Elapsed: ${
                                        formatHours(incidentDetails!!.incident.hoursSpent)
                                    }",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Deadline: ${
                                        formatDateTime(incidentDetails!!.incident.targetResolutionTime)
                                    }",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,

                                ) {

                                    Text(
                                        text = "Status : ",
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                if (incidentDetails!!.incident.isSlaBreached) {
                                                    "BREACHED"
                                                } else {
                                                    "WITHIN SLA"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // DESCRIPTION

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "DESCRIPTION",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = incidentDetails!!.incident.description ?: "-",
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        // AFFECTED ASSET DETAILS

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "AFFECTED ASSET DETAILS",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            // ASSET CODE

                            OutlinedCard(
                                modifier = Modifier.weight(1f)
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Text(
                                        text = "ASSET CODE",
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(
                                        text =
                                            incidentDetails!!.incident.assetCodeCurrent
                                                ?: incidentDetails!!.incident.assetCodeSnapshot
                                                ?: "-",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // ASSET NAME

                            OutlinedCard(
                                modifier = Modifier.weight(1f)
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Text(
                                        text = "ASSET NAME",
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(
                                        text =
                                            incidentDetails!!.incident.assetNameCurrent
                                                ?: incidentDetails!!.incident.assetNameSnapshot
                                                ?: "-",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // AFFECTED PORTS / CORES

                        if (affectedPorts.isNotEmpty() || affectedCores.isNotEmpty()) {

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = if (affectedPorts.isNotEmpty()) {
                                    "AFFECTED PORTS (${affectedPorts.size})"
                                } else {
                                    "AFFECTED CORES (${affectedCores.size})"
                                },
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (affectedPorts.isNotEmpty()) {

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {

                                    items(
                                        items = affectedPorts,
                                        key = { it.portId }
                                    ) { port ->

                                        OutlinedCard(
                                            modifier = Modifier.width(220.dp)
                                        ) {

                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {

                                                Text(
                                                    text = "Port ${port.portNo}",
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(8.dp)
                                                )

                                                Text(
                                                    text = "Type: ${port.portType ?: "-"}"
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(4.dp)
                                                )

                                                Text(
                                                    text = "Asset: ${port.assetCode ?: "-"}"
                                                )
                                            }
                                        }
                                    }
                                }

                            } else if (affectedCores.isNotEmpty()) {

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {

                                    items(
                                        items = affectedCores,
                                        key = { it.fiberCoreId }
                                    ) { core ->

                                        OutlinedCard(
                                            modifier = Modifier.width(220.dp)
                                        ) {

                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {

                                                Text(
                                                    text = "Core ${core.coreNo}",
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(8.dp)
                                                )

                                                Text(
                                                    text = "Tube: ${core.tubeNo}"
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(4.dp)
                                                )

                                                Text(
                                                    text = "Asset: ${core.assetCode ?: "-"}"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // IMPACTED NETWORK ASSETS

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "IMPACTED NETWORK ASSETS (${impactAssets.size})",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (impactAssets.isEmpty()) {

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No impacted assets found")
                                }
                            }

                        } else {

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                items(
                                    count = impactAssets.size
                                ) { index ->

                                    val asset = impactAssets[index]

                                    OutlinedCard(
                                        modifier = Modifier.width(280.dp)
                                    ) {

                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {

                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {

                                                    Text(
                                                        text = asset.assetCode ?: "-",
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    Text(
                                                        text = asset.assetName ?: "-",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                AssistChip(
                                                    onClick = {},
                                                    label = {
                                                        Text(
                                                            asset.severityLevel ?: "-"
                                                        )
                                                    }
                                                )
                                            }

                                            Spacer(
                                                modifier = Modifier.height(12.dp)
                                            )

                                            HorizontalDivider()

                                            Spacer(
                                                modifier = Modifier.height(12.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {

                                                Text(
                                                    text = "Impact"
                                                )

                                                Text(
                                                    text = "${asset.impactPercentage ?: 0}%"
                                                )
                                            }

                                            Spacer(
                                                modifier = Modifier.height(6.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {

                                                Text(
                                                    text = "Capacity"
                                                )

                                                Text(
                                                    text = "${asset.affectedCapacity ?: 0} / ${asset.totalCapacity ?: 0}"
                                                )
                                            }

                                            Spacer(
                                                modifier = Modifier.height(6.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {

                                                Text(
                                                    text = "Depth"
                                                )

                                                Text(
                                                    text = "${asset.depth ?: 0}"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // GPS LOCATION

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "COORDINATES / GPS LOCATION",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Open location in Google Maps",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {

                                            val latitude =
                                                incidentDetails!!.incident.latitude

                                            val longitude =
                                                incidentDetails!!.incident.longitude

                                            if (latitude != null && longitude != null) {

                                                val uri =
                                                    "geo:$latitude,$longitude?q=$latitude,$longitude".toUri()

                                                val intent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    uri
                                                )

                                                intent.setPackage(
                                                    "com.google.android.apps.maps"
                                                )

                                                context.startActivity(intent)
                                            }
                                        }
                                )

                                Spacer(
                                    modifier = Modifier.width(16.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = "Lat: ${
                                            incidentDetails!!.incident.latitude ?: "-"
                                        }"
                                    )

                                    Spacer(
                                        modifier = Modifier.height(4.dp)
                                    )

                                    Text(
                                        text = "Long: ${
                                            incidentDetails!!.incident.longitude ?: "-"
                                        }"
                                    )
                                }
                            }
                        }

                        // ATTACHMENTS

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "ATTACHMENTS (${incidentDetails!!.attachments.size})",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (incidentDetails!!.attachments.isEmpty()) {

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No attachments uploaded.")
                                }
                            }

                        } else {

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                incidentDetails!!.attachments.forEach { attachment ->

                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            // FILE TYPE ICON

                                            Icon(
                                                imageVector =
                                                    if (attachment.fileName
                                                            .lowercase()
                                                            .endsWith(".pdf")
                                                    ) {
                                                        Icons.Default.PictureAsPdf
                                                    } else {
                                                        Icons.Default.Image
                                                    },
                                                contentDescription = "Attachment",
                                                modifier = Modifier.size(32.dp)
                                            )

                                            Spacer(
                                                modifier = Modifier.width(12.dp)
                                            )

                                            // FILE DETAILS

                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {

                                                Text(
                                                    text = attachment.fileName,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(4.dp)
                                                )

                                                Text(
                                                    text = "By ${attachment.uploadedByUserName} • ${
                                                        formatDateTime(
                                                            attachment.createdOn
                                                        )
                                                    }",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // VIEW

                                            IconButton(
                                                onClick = {
                                                    val fileUrl =
                                                        BASE_URL + attachment.filePath.removePrefix("/")

                                                    viewAttachment(
                                                        context,
                                                        fileUrl
                                                    )
                                                }
                                            ) {

                                                Icon(
                                                    imageVector = Icons.Default.Visibility,
                                                    contentDescription = "View"
                                                )
                                            }

//                                            // DOWNLOAD
//
//                                            IconButton(
//                                                onClick = {
//                                                    val fileUrl =
//                                                        BASE_URL + attachment.filePath.removePrefix("/")
//
//                                                    downloadAttachment(
//                                                        context = context,
//                                                        fileUrl = fileUrl,
//                                                        fileName = attachment.fileName
//                                                    )
//                                                }
//                                            ) {
//
//                                                Icon(
//                                                    imageVector = Icons.Default.Download,
//                                                    contentDescription = "Download"
//                                                )
//                                            }
//
//                                            // DELETE
//
//                                            IconButton(
//                                                onClick = {
//                                                    // Delete attachment
//                                                }
//                                            ) {
//
//                                                Icon(
//                                                    imageVector = Icons.Default.Delete,
//                                                    contentDescription = "Delete"
//                                                )
//                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("📁 Project: ${incidentDetails!!.incident.projectName}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("📅 Logged On: ${formatDateTime(incidentDetails!!.incident.createdOn)} by ${incidentDetails!!.incident.reportedByUserName}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("⏰ Target SLA Deadline: ${formatDateTime(incidentDetails!!.incident.targetResolutionTime)}")

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider()
                    }
                }
            }
        }
        if (showCommentsDialog) {

            Dialog(
                onDismissRequest = {
                    showCommentsDialog = false
                },
            ) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(20.dp),
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .navigationBarsPadding()
                        ) {

                            // HEADER

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = "Timeline / Audit Log",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        showCommentsDialog = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close"
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

                            // COMMENTS

                            if (incidentDetails?.comments.isNullOrEmpty()) {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No comments available")
                                }

                            } else {

                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {

                                    items(
                                        incidentDetails!!.comments,
                                        key = { it.commentId }
                                    ) { comment ->

                                        OutlinedCard(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {

                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {

                                                // USER + DATE

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {

                                                    Icon(
                                                        imageVector =
                                                            Icons.Default.AccountCircle,
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier.size(38.dp)
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
                                                                comment.userName ?: "-",
                                                            fontWeight =
                                                                FontWeight.Bold
                                                        )

                                                        Text(
                                                            text =
                                                                formatDateTime(
                                                                    comment.createdOn
                                                                ),
                                                            style =
                                                                MaterialTheme
                                                                    .typography
                                                                    .bodySmall
                                                        )
                                                    }
                                                }

                                                // STATUS

                                                comment.statusChangedTo?.let { status ->

                                                    Spacer(
                                                        modifier =
                                                            Modifier.height(8.dp)
                                                    )

                                                    AssistChip(
                                                        onClick = {},
                                                        label = {
                                                            Text(
                                                                "Status: $status"
                                                            )
                                                        }
                                                    )
                                                }

                                                Spacer(
                                                    modifier =
                                                        Modifier.height(8.dp)
                                                )

                                                // COMMENT

                                                Text(
                                                    text =
                                                        comment.commentText ?: "-"
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            // ADD COMMENT

                            OutlinedTextField(
                                value = commentText,
                                onValueChange = {
                                    commentText = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Add Comment")
                                },
                                placeholder = {
                                    Text("Write your comment...")
                                },
                                minLines = 2,
                                maxLines = 4
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Button(
                                onClick = {

                                    if (commentText.isNotBlank()) {

                                        viewModel.addComment(
                                            incidentId = incidentId,
                                            commentText = commentText.trim()
                                        ) {
                                            commentText = ""
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = commentText.isNotBlank()
                            ) {
                                Text("Post Comment")
                            }
                        }
                        if (isPostingComment) {

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
                                            text = "Posting Comment..."
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatHours(hours: Double?): String {

    if (hours == null || hours <= 0) {
        return "0m"
    }

    val totalMinutes = (hours * 60).toInt()

    val days = totalMinutes / (24 * 60)
    val remainingMinutes = totalMinutes % (24 * 60)

    val h = remainingMinutes / 60
    val m = remainingMinutes % 60

    return buildList {
        if (days > 0) {
            add("${days}d")
        }

        if (h > 0) {
            add("${h}h")
        }

        if (m > 0) {
            add("${m}m")
        }
    }.joinToString(" ")
}

fun formatDateTime(value: String?): String {

    if (value.isNullOrBlank()) {
        return "-"
    }

    return try {

        val inputFormatter =
            DateTimeFormatter.ofPattern(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
            )

        val outputFormatter =
            DateTimeFormatter.ofPattern(
                "dd/MM/yyyy, hh:mm:ss a"
            )

        LocalDateTime
            .parse(value, inputFormatter)
            .format(outputFormatter)

    } catch (e: Exception) {

        value
    }
}

fun viewAttachment(
    context: Context,
    fileUrl: String
) {
    try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            fileUrl.toUri()
        )

        context.startActivity(intent)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun downloadAttachment(
    context: Context,
    fileUrl: String,
    fileName: String
) {

    val request =
        DownloadManager.Request(
            fileUrl.toUri()
        )

    request.setTitle(fileName)

    request.setDescription(
        "Downloading attachment"
    )

    request.setNotificationVisibility(
        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
    )

    request.setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOWNLOADS,
        fileName
    )

    val downloadManager =
        context.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager

    downloadManager.enqueue(request)
}