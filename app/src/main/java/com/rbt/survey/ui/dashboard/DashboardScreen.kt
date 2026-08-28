package com.rbt.survey.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.rbt.survey.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onInventoryClick: () -> Unit,
    onSurveyClick: () -> Unit,
    onLocationTrackingClick: () -> Unit,
    onNavigateToDgpsSettings: () -> Unit,
    onIncidentManagementClick: () -> Unit,
    onInspectionAuditClick: () -> Unit,
    onLogout: () -> Unit
) {

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {

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
        }
    ) { paddingValues ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                DashboardCard(
                    title = "Inventory",
                    icon = Icons.Default.Inventory,
                    onClick = onInventoryClick
                )
            }

            item {
                DashboardCard(
                    title = "Survey",
                    imageRes = R.drawable.survey,
                    onClick = onSurveyClick
                )
            }

            item {
                DashboardCard(
                    title = "Location Tracking",
                    imageRes = R.drawable.location_track,
                    onClick = onLocationTrackingClick
                )
            }

            item {
                DashboardCard(
                    title = "Device",
                    imageRes = R.drawable.device_connection,
                    onClick = onNavigateToDgpsSettings
                )
            }

            item {
                DashboardCard(
                    title = "Incident Management",
                    imageRes = R.drawable.incident,
                    onClick = onIncidentManagementClick
                )
            }

            item {
                DashboardCard(
                    title = "Inspection Audit",
                    imageRes = R.drawable.inspection,
                    onClick = onInspectionAuditClick
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector? = null,
    imageRes: Int? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                imageRes != null -> {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = title,
                        modifier = Modifier.size(72.dp)
                    )
                }

                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
