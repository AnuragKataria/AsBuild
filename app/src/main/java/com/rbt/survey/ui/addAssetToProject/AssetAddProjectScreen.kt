//package com.rbt.survey.ui.addAssetToProject
//
//import android.util.Log
//import android.widget.Toast
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.ArrowDropDown
//import androidx.compose.material.icons.filled.Search
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.*
//import coil.compose.AsyncImage
//import com.rbt.survey.data.model.*
//import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AssetAddProjectScreen(
//    onBackClick: () -> Unit,
////    onvieweditassetClick: () -> Unit,
//    onaddassetClick: () -> Unit,
//    viewModel: AssetAddProjectViewModel
//) {
//
//    val base_URL = "https://webgis.rbt-ltd.com/api"
//    val projects by viewModel.projects.collectAsState()
//    val states by viewModel.states.collectAsState()
//    val districts by viewModel.districts.collectAsState()
//    val blocks by viewModel.blocks.collectAsState()
//    val assetDetails by viewModel.assetDetails.collectAsState()
//
//    var selectedProject by remember { mutableStateOf<ProjectResponse?>(null) }
//    var selectedStates by remember { mutableStateOf<List<DropdownItem>>(emptyList()) }
//    var selectedDistricts by remember { mutableStateOf<List<DropdownItem>>(emptyList()) }
//    var selectedBlocks by remember { mutableStateOf<List<DropdownItem>>(emptyList()) }
//    var selectedAsset by remember { mutableStateOf<AssetDetailResponse?>(null) }
//
//    val isLoading by viewModel.isLoading.collectAsState()
//    val isAssetLoading by viewModel.isAssetLoading.collectAsState()
//
//    var showStateDialog by remember { mutableStateOf(false) }
//    var showDistrictDialog by remember { mutableStateOf(false) }
//    var showBlockDialog by remember { mutableStateOf(false) }
//
//    val projectNames = projects.map { it.projectName }
//
//    var showAssetDialog by remember { mutableStateOf(false) }
//
//    val isViewEditEnabled = selectedProject != null || selectedStates.isNotEmpty() || selectedDistricts.isNotEmpty() || selectedBlocks.isNotEmpty()
//    val isAddAssetEnabled = selectedProject != null && selectedStates.isNotEmpty() && selectedDistricts.isNotEmpty() && selectedBlocks.isNotEmpty() && selectedAsset != null
//
//    LaunchedEffect(Unit) {
//        viewModel.loadProjects()
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text("Asset Details")
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
//        },
//        bottomBar = {
//            Row(
//                modifier = Modifier.fillMaxWidth().padding(16.dp),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//
////                Button(
////                    onClick = {
////                        // View/Edit Asset
////                    },
////                    enabled = isViewEditEnabled,
////                    modifier = Modifier
////                        .weight(1f)
////                        .height(52.dp),
////                    shape = RoundedCornerShape(14.dp)
////                ) {
////                    Text("View/Edit Asset")
////                }
//
//                Button(
//                    onClick = {
//                        // Add Asset
//                    },
//                    enabled = isAddAssetEnabled,
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(52.dp),
//                    shape = RoundedCornerShape(14.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color(0xFFC79AF8)
//                    )
//                ) {
//                    Icon(
//                        Icons.Default.Add,
//                        contentDescription = null
//                    )
//
//                    Spacer(modifier = Modifier.width(4.dp))
//
//                    Text("Add Asset")
//                }
//            }
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp)
//                .verticalScroll(rememberScrollState())
//        ) {
//
//            Text(
//                text = "Project",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Gray
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            CustomDropdown(
//                selectedValue = selectedProject?.projectName ?: "",
//                placeholder = "Select Project",
//                items = projectNames,
//                onValueSelected = { projectName ->
//
//                    selectedProject =
//                        projects.find {
//                            it.projectName == projectName
//                        }
//
//                    selectedStates = emptyList()
//                    selectedDistricts = emptyList()
//                    selectedBlocks = emptyList()
//
//                    selectedProject?.projectId?.let {
//                        viewModel.loadStates(it)
//                    }
//
//                }
//            )
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            Text(
//                text = "State",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Gray
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            MultiSelectDropdown(
//                items = states,
//                selectedItems = selectedStates,
//                onSelectionChanged = {selectedStates = it
//
//                    selectedDistricts = emptyList()
//                    selectedBlocks = emptyList()
//
//                    val projectId =
//                        selectedProject?.projectId
//
//                    if (
//                        projectId != null &&
//                        selectedStates.isNotEmpty()
//                    ) {
//
//                        viewModel.loadDistricts(
//                            projectId = projectId,
//                            stateCodes = selectedStates.map {
//                                it.value
//                            }
//                        )
//                    }
//                }
//            )
//            if (showStateDialog) {
//
//                MultiSelectDialog(
//                    title = "Select States",
//                    items = states,
//                    selectedItems = selectedStates,
//                    onDismiss = {
//                        showStateDialog = false
//                    },
//                    onDone = { selection ->
//
//                        selectedStates = selection
//                        selectedDistricts = emptyList()
//                        selectedBlocks = emptyList()
//
//                        showStateDialog = false
//
//
//                    }
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            Text(
//                text = "District",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Gray
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            MultiSelectDropdown(
//                items = districts,
//                selectedItems = selectedDistricts,
//                onSelectionChanged = {
//                    selectedDistricts = it
//                    selectedBlocks = emptyList()
//
//                    val projectId =
//                        selectedProject?.projectId
//
//                    if (
//                        projectId != null &&
//                        selectedStates.isNotEmpty() &&
//                        selectedDistricts.isNotEmpty()
//                    ) {
//
//                        viewModel.loadBlocks(
//                            projectId = projectId,
//                            stateCodes = selectedStates.map { state ->
//                                state.value
//                            },
//                            districtCodes = selectedDistricts.map { district ->
//                                district.value
//                            }
//                        )
//                    }
//                }
//            )
//            if (showDistrictDialog) {
//
//                MultiSelectDialog(
//                    title = "Select Districts",
//                    items = districts,
//                    selectedItems = selectedDistricts,
//                    onDismiss = {
//                        showDistrictDialog = false
//                    },
//                    onDone = { selection ->
//
//                        selectedDistricts = selection
//                        selectedBlocks = emptyList()
//
//                        showDistrictDialog = false
//                    }
//                )
//            }
//
//            Spacer(modifier = Modifier.height(12.dp))
//
//            Text(
//                text = "Block",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Gray
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            MultiSelectDropdown(
//                items = blocks,
//                selectedItems = selectedBlocks,
//                onSelectionChanged = {
//                    selectedBlocks = it
//                }
//            )
//            if (showBlockDialog) {
//
//                MultiSelectDialog(
//                    title = "Select Blocks",
//                    items = blocks,
//                    selectedItems = selectedBlocks,
//                    onDismiss = {
//                        showBlockDialog = false
//                    },
//                    onDone = { selection ->
//                        selectedBlocks = selection
//                        showBlockDialog = false
//                    }
//                )
//            }
//
//            Spacer(modifier = Modifier.height(18.dp))
//
//            Text(
//                text = "Asset",
//                fontSize = 12.sp,
//                fontWeight = FontWeight.Medium,
//                color = Color.Gray
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable {
//
//                        viewModel.loadAssets()
//                        showAssetDialog = true
//                    },
//                shape = RoundedCornerShape(12.dp),
//                border = BorderStroke(
//                    1.dp,
//                    Color.LightGray
//                )
//            ) {
//
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//
//                    Text(
//                        text =
//                            selectedAsset?.data?.assetName
//                                ?: "Select Asset",
//                        modifier = Modifier.weight(1f)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//        }
//        if (showAssetDialog) {
//
//            AlertDialog(
//                onDismissRequest = {
//                    showAssetDialog = false
//                },
//                title = {
//                    Text("Select Asset")
//                },
//                text = {
//
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(400.dp)
//                    ) {
//                        if (isAssetLoading) {
//
//                            Box(
//                                modifier = Modifier.fillMaxSize(),
//                                contentAlignment = Alignment.Center
//                            ) {
//
//                                CircularProgressIndicator()
//                            }
//
//                        } else {
//                            LazyVerticalGrid(
//                                columns = GridCells.Fixed(3),
//                                modifier = Modifier.fillMaxSize()
//                            ) {
//
//                                items(assetDetails) { asset ->
//
//                                    Column(
//                                        modifier = Modifier
//                                            .padding(8.dp)
//                                            .clickable {
//
//                                                selectedAsset = asset
//                                                showAssetDialog = false
//                                            },
//                                        horizontalAlignment = Alignment.CenterHorizontally
//                                    ) {
//
//                                        AsyncImage(
//                                            model =
//                                                base_URL + (asset.data.imageUrl ?: ""),
//                                            contentDescription = null,
//                                            modifier = Modifier.size(70.dp)
//                                        )
//
//                                        Spacer(
//                                            modifier = Modifier.height(4.dp)
//                                        )
//
//                                        Text(
//                                            text = asset.data.assetName,
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                },
//                confirmButton = {},
//                dismissButton = {
//                    TextButton(
//                        onClick = {
//                            showAssetDialog = false
//                        }
//                    ) {
//                        Text("Close")
//                    }
//                }
//            )
//        }
//        if (isLoading) {
//
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(
//                        Color.Black.copy(alpha = 0.3f)
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//
//                CircularProgressIndicator()
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CustomDropdown(
//    selectedValue: String,
//    placeholder: String,
//    items: List<String>,
//    onValueSelected: (String) -> Unit
//) {
//
//    var expanded by remember { mutableStateOf(false) }
//
//    ExposedDropdownMenuBox(
//        expanded = expanded,
//        onExpandedChange = { expanded = !expanded }
//    ) {
//
//        OutlinedTextField(
//            value = selectedValue,
//            onValueChange = {},
//            readOnly = true,
//            placeholder = {
//                Text(placeholder)
//            },
//            trailingIcon = {
//                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .menuAnchor(),
//            shape = RoundedCornerShape(14.dp)
//        )
//
//        ExposedDropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//            items.forEach { item ->
//                DropdownMenuItem(
//                    text = { Text(item) },
//                    onClick = {
//                        onValueSelected(item)
//                        expanded = false
//                    }
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MultiSelectDropdown(
//    items: List<DropdownItem>,
//    selectedItems: List<DropdownItem>,
//    onSelectionChanged: (List<DropdownItem>) -> Unit
//) {
//
//    var expanded by remember { mutableStateOf(false) }
//
//    ExposedDropdownMenuBox(
//        expanded = expanded,
//        onExpandedChange = { expanded = !expanded }
//    ) {
//
//        OutlinedTextField(
//            value = selectedItems.joinToString(", ") { it.label },
//            onValueChange = {},
//            readOnly = true,
//            label = { Text("Select States") },
//            trailingIcon = {
//                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
//            },
//            modifier = Modifier
//                .fillMaxWidth()
//                .menuAnchor()
//        )
//
//        ExposedDropdownMenu(
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//
//            items.forEach { item ->
//
//                val selected =
//                    selectedItems.any {
//                        it.value == item.value
//                    }
//
//                DropdownMenuItem(
//                    text = {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//
//                            Checkbox(
//                                checked = selected,
//                                onCheckedChange = null
//                            )
//
//                            Text(item.label)
//                        }
//                    },
//                    onClick = {
//
//                        val updated =
//                            if (selected) {
//                                selectedItems.filter {
//                                    it.value != item.value
//                                }
//                            } else {
//                                selectedItems + item
//                            }
//
//                        onSelectionChanged(updated)
//                    }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun MultiSelectDialog(
//    title: String,
//    items: List<DropdownItem>,
//    selectedItems: List<DropdownItem>,
//    onDismiss: () -> Unit,
//    onDone: (List<DropdownItem>) -> Unit
//) {
//
//    var tempSelection by remember {
//        mutableStateOf(selectedItems)
//    }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = {
//            Text(title)
//        },
//        text = {
//
//            LazyColumn {
//
//                items(items) { item ->
//
//                    val isSelected =
//                        tempSelection.any {
//                            it.value == item.value
//                        }
//
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clickable {
//
//                                tempSelection =
//                                    if (isSelected) {
//                                        tempSelection.filter {
//                                            it.value != item.value
//                                        }
//                                    } else {
//                                        tempSelection + item
//                                    }
//                            }
//                            .padding(vertical = 8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//
//                        Checkbox(
//                            checked = isSelected,
//                            onCheckedChange = {
//
//                                tempSelection =
//                                    if (isSelected) {
//                                        tempSelection.filter {
//                                            it.value != item.value
//                                        }
//                                    } else {
//                                        tempSelection + item
//                                    }
//                            }
//                        )
//
//                        Text(item.label)
//                    }
//                }
//            }
//        },
//        confirmButton = {
//
//            TextButton(
//                onClick = {
//                    onDone(tempSelection)
//                }
//            ) {
//                Text("Done")
//            }
//        },
//        dismissButton = {
//
//            TextButton(
//                onClick = onDismiss
//            ) {
//                Text("Cancel")
//            }
//        }
//    )
//}