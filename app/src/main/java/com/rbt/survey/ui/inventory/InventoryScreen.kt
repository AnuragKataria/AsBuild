package com.rbt.survey.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbt.survey.ui.dashboard.DashboardCard
import com.rbt.survey.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAssetManagementClick: () -> Unit,
    onAddAssetsToProjectClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Inventory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            DashboardCard(
                title = "Asset Management",
                imageRes = R.drawable.asset_management,
                onClick = onAssetManagementClick
            )

            DashboardCard(
                title = "Add Assets To Project",
                imageRes = R.drawable.add_asset,
                onClick = onAddAssetsToProjectClick
            )
        }
    }
}