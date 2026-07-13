package com.rbt.survey.ui.assetManagement


import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.rbt.survey.data.model.CreateAssetRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetManagementScreen(
    onBackClick: () -> Unit,
    onAssetClick: (Int) -> Unit,
    viewModel: AssetManagementViewModel
) {

    val context = LocalContext.current

    var searchText by remember { mutableStateOf("") }

    val assetTypes by viewModel.assetTypes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val filteredList = assetTypes.filter {
        it.assetName.contains(searchText, ignoreCase = true)
    }

    var showCreateAssetDialog by remember {
        mutableStateOf(false)
    }

    val createSuccess by viewModel.createSuccess.collectAsState()
    val message by viewModel.message.collectAsState()

    val isCreating by viewModel.isCreating.collectAsState()


    LaunchedEffect(createSuccess) {

        if (createSuccess) {

            showCreateAssetDialog = false
            viewModel.resetCreateSuccess()
        }
    }

    LaunchedEffect(message) {

        message?.let {

            Toast.makeText(
                context,
                it,
                Toast.LENGTH_SHORT
            ).show()

            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Asset Management") },
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
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "ASSET TYPES",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                FilledIconButton(
                    onClick = {
                    showCreateAssetDialog = true
                },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search...")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    items(filteredList) { asset ->

                        ListItem(
                            headlineContent = {
                                Text(asset.assetName)
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAssetClick(asset.assetTypeID)
                                }
                        )

                        HorizontalDivider()
                    }
                }
            }

            if (showCreateAssetDialog) {
                CreateAssetTypeDialog(
                    onDismiss = {
                        showCreateAssetDialog = false
                    },
                    onCreate = { code,
                                 name,
                                 category,
                                 nature,
                                 geometry,
                                 imageUri ->

                        val request = CreateAssetRequest(
                            assetCode = code,
                            assetName = name,
                            assetCategory = category,
                            assetnature = nature,
                            geometryType = geometry
                        )

                        if (imageUri == null) {

                            viewModel.createAsset(
                                request
                            )

                        } else {

                            val imagePart =
                                createImagePart(
                                    context,
                                    imageUri
                                )

                            viewModel.createAssetWithImage(
                                request,
                                imagePart
                            )
                        }
                    }
                )
            }

            if (isCreating) {

                Dialog(
                    onDismissRequest = { }
                ) {

                    Card(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator()

                            Spacer(modifier = Modifier.width(16.dp))

                            Text("Creating Asset...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateAssetTypeDialog(
    onDismiss: () -> Unit,
    onCreate: (
        String,
        String,
        String,
        String,
        String,
        Uri?
    ) -> Unit
) {

    var assetCode by remember { mutableStateOf("") }
    var assetName by remember { mutableStateOf("") }

    var selectedCategory by remember {
        mutableStateOf("Select Category")
    }
    var selectedNature by remember {
        mutableStateOf("Select Nature")
    }
    var selectedGeometry by remember {
        mutableStateOf("Select Geometry Type")
    }
    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            selectedImageUri = uri
        }

    var assetCodeError by remember { mutableStateOf(false) }
    var assetNameError by remember { mutableStateOf(false) }
    var categoryError by remember { mutableStateOf(false) }
    var natureError by remember { mutableStateOf(false) }
    var geometryError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Asset Type")
        },
        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = assetCode,
                    onValueChange = {
                        assetCode = it
                        assetCodeError = false
                    },
                    label = {
                        Text("Asset Code")
                    },
                    isError = assetCodeError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (assetCodeError) {
                    Text(
                        text = "Asset Code is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = assetName,
                    onValueChange = {
                        assetName = it
                        assetNameError = false
                    },
                    label = {
                        Text("Asset Name")
                    },
                    isError = assetNameError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (assetNameError) {
                    Text(
                        text = "Asset Name is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                CommonDropdown(
                    label = "Category",
                    options = listOf(
                        "Select Category",
                        "CIVIL",
                        "PASSIVE",
                        "ACTIVE",
                        "POWER"
                    ),
                    selectedValue = selectedCategory,
                    onValueSelected = {
                        selectedCategory = it
                    }
                )
                if (categoryError) {
                    Text(
                        text = "Category is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                CommonDropdown(
                    label = "Asset Nature",
                    options = listOf(
                        "Select Nature",
                        "LINEAR",
                        "NODE",
                        "LOGICAL",
                        "EQUIPMENT"
                    ),
                    selectedValue = selectedNature,
                    onValueSelected = {
                        selectedNature = it
                    }
                )
                if (natureError) {
                    Text(
                        text = "Asset Nature is required",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                CommonDropdown(
                    label = "Geometry Type",
                    options = listOf(
                        "Select Geometry Type",
                        "Line",
                        "POINT"
                    ),
                    selectedValue = selectedGeometry,
                    onValueSelected = {
                        selectedGeometry = it
                    }
                )
                if (geometryError) {
                    Text(
                        text = "Geometry Type is required",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose Asset Image")
                }

                selectedImageUri?.let { uri ->

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {

            Button(
                onClick = {
                    assetCodeError = assetCode.isBlank()
                    assetNameError = assetName.isBlank()
                    categoryError = selectedCategory == "Select Category"
                    natureError = selectedNature == "Select Nature"
                    geometryError = selectedGeometry == "Select Geometry Type"

                    if (
                        !assetCodeError &&
                        !assetNameError &&
                        !categoryError &&
                        !natureError &&
                        !geometryError
                    ) {
                        onCreate(
                            assetCode,
                            assetName,
                            selectedCategory,
                            selectedNature,
                            selectedGeometry,
                            selectedImageUri
                        )
                    }
                }
            ) {
                Text("Create Asset Type")
            }
        },
        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonDropdown(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

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
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
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
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun createImagePart(
    context: Context,
    uri: Uri
): MultipartBody.Part {

    val bytes =
        context.contentResolver
            .openInputStream(uri)
            ?.readBytes()
            ?: ByteArray(0)

    val requestBody =
        bytes.toRequestBody(
            "image/*".toMediaTypeOrNull()
        )

    return MultipartBody.Part.createFormData(
        "Image",
        "asset.jpg",
        requestBody
    )
}