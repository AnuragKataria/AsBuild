//package com.rbt.survey.ui.addAssetToProject
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.LocationOn
//import androidx.compose.material.icons.filled.Upload
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.*
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ViewEditAssetScreen(
//    onBackClick: () -> Unit
//) {
//
//    var selectedTab by remember {
//        mutableStateOf(0)
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text("View / Edit Assets")
//                },
//                navigationIcon = {
//                    IconButton(
//                        onClick = onBackClick
//                    ) {
//                        Icon(
//                            Icons.Default.ArrowBack,
//                            contentDescription = null
//                        )
//                    }
//                }
//            )
//        }
//    ) { padding ->
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//
//            // Tabs
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(12.dp)
//            ) {
//
//                listOf(
//                    "Assets",
//                    "Planned",
//                    "Survey"
//                ).forEachIndexed { index, title ->
//
//                    FilterChip(
//                        selected = selectedTab == index,
//                        onClick = {
//                            selectedTab = index
//                        },
//                        label = {
//                            Text(title)
//                        },
//                        modifier = Modifier.padding(end = 8.dp)
//                    )
//                }
//            }
//
//            // Assets Panel Header
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(
//                        horizontal = 16.dp,
//                        vertical = 12.dp
//                    ),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                Text(
//                    text = "ASSETS PANEL",
//                    fontWeight = FontWeight.Bold,
//                    color = Color.Gray,
//                    letterSpacing = 1.sp,
//                    modifier = Modifier.weight(1f)
//                )
//
//                OutlinedIconButton(
//                    onClick = {
//                        // later
//                    }
//                ) {
//                    Icon(
//                        Icons.Default.Visibility,
//                        contentDescription = null
//                    )
//                }
//            }
//
//            HorizontalDivider()
//
//            // Empty State
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f),
//                contentAlignment = Alignment.Center
//            ) {
//
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//
//                    OutlinedIconButton(
//                        onClick = {}
//                    ) {
//                        Icon(
//                            Icons.Default.LocationOn,
//                            contentDescription = null
//                        )
//                    }
//
//                    Spacer(
//                        modifier = Modifier.height(12.dp)
//                    )
//
//                    Text(
//                        text = "No assets yet",
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Gray
//                    )
//
//                    Text(
//                        text = "Add an asset to see it here",
//                        color = Color.LightGray
//                    )
//                }
//            }
//
//            HorizontalDivider()
//
//            // Imported Layers
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//
//                Icon(
//                    Icons.Default.Upload,
//                    contentDescription = null,
//                    tint = Color(0xFF2196F3)
//                )
//
//                Spacer(
//                    modifier = Modifier.width(8.dp)
//                )
//
//                Text(
//                    text = "IMPORTED LAYERS",
//                    fontWeight = FontWeight.Bold,
//                    color = Color.Gray
//                )
//
//                Spacer(
//                    modifier = Modifier.width(8.dp)
//                )
//
//                Badge {
//                    Text("0")
//                }
//            }
//
//            Spacer(
//                modifier = Modifier.height(16.dp)
//            )
//        }
//    }
//}