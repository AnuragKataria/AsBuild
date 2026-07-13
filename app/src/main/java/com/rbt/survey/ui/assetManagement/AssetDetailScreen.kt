package com.rbt.survey.ui.assetManagement

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.platform.LocalContext
import com.rbt.survey.data.model.*
import sh.calvin.reorderable.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    assetTypeId: Int,
    onBackClick: () -> Unit,
    viewModel: AssetDetailViewModel
) {

    val context = LocalContext.current
    val base_URL = "https://webgis.rbt-ltd.com/api"

    LaunchedEffect(assetTypeId) {
        viewModel.loadData(assetTypeId)
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val assetDetail by viewModel.assetDetail.collectAsState()
    val canvasFields by viewModel.canvasFields.collectAsState()
    val formFieldMasters by viewModel.formFieldMasters.collectAsState()
    val assetConfig by viewModel.assetConfig.collectAsState()
    val lazyListState = rememberLazyListState()

    var selectedField by remember { mutableStateOf<DynamicField?>(null) }
    var showCustomFieldDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<FieldMaster?>(null) }
    var fieldCode by remember { mutableStateOf("") }
    var fieldLabel by remember {mutableStateOf("") }
    var fieldCodeError by remember { mutableStateOf(false) }
    var fieldLabelError by remember { mutableStateOf(false) }
    var dataTypeError by remember { mutableStateOf(false) }
    var isFieldMasterExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var dataTypeExpanded by remember { mutableStateOf(false) }
    var selectedDataType by remember { mutableStateOf("") }
    var editingField by remember { mutableStateOf<DynamicField?>(null) }
    var required by remember { mutableStateOf(false) }
    var searchable by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val dataTypes = formFieldMasters
        .map { it.fieldType }
        .distinct()
        .sorted()

    val reorderableState =
        rememberReorderableLazyListState(
            lazyListState = lazyListState
        ) { from, to ->

            viewModel.moveField(
                from.index,
                to.index
            )
        }

    val filteredFields =
        formFieldMasters.filter {

            it.fieldName.contains(
                searchText,
                ignoreCase = true
            ) ||

                    it.fieldCode.contains(
                        searchText,
                        ignoreCase = true
                    )
        }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Asset Details")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {

            if (canvasFields.isNotEmpty()) {
                Button(
                    onClick = {

                        assetDetail?.data?.let { asset ->

                            viewModel.saveConfiguration(

                                assetTypeId = asset.assetTypeId,

                                assetCode = asset.assetCode,

                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        "Configuration saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },

                                onError = { error ->

                                    Toast.makeText(
                                        context,
                                        error,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 4.dp,
                            bottom = 12.dp
                        ),
                ) {
                    Text("Save Configuration")
                }
            }
        }
    ) { padding ->

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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {

                    assetDetail?.data?.let { asset ->

                        val fullImageUrl =
                            base_URL + asset.imageUrl
                        Log.d(
                            "IMAGE_URL",
                            fullImageUrl
                        )
                        LazyRow(
                            modifier = Modifier.padding(
                                start = 12.dp,
                                end = 12.dp,
                                top = 12.dp,
                                bottom = 4.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            item {
                                InfoTile(
                                    title = "ASSET CODE",
                                    value = asset.assetCode,
                                    icon = Icons.Default.Tag
                                )
                            }

                            item {
                                InfoTile(
                                    title = "NAME",
                                    value = asset.assetName,
                                    imageUrl = fullImageUrl
                                )
                            }

                            item {
                                InfoTile(
                                    title = "CATEGORY",
                                    value = asset.assetCategory,
                                    icon = Icons.Default.GridView
                                )
                            }

                            item {
                                InfoTile(
                                    title = "GEOMETRY",
                                    value = asset.geometryType,
                                    icon = Icons.Default.Tune
                                )
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {

                        Column {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isFieldMasterExpanded =
                                            !isFieldMasterExpanded
                                    }
                                    .padding(16.dp),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text = "Field Masters",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Icon(
                                    imageVector =
                                        if (isFieldMasterExpanded)
                                            Icons.Default.KeyboardArrowUp
                                        else
                                            Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }


                            if (isFieldMasterExpanded) {

                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = {
                                        searchText = it
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    placeholder = {
                                        Text("Search Templates...")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null
                                        )
                                    },
                                    singleLine = true
                                )


                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )


                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .padding(horizontal = 12.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    items(filteredFields) { field ->

                                        Card(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement =
                                                    Arrangement.SpaceBetween,
                                                verticalAlignment =
                                                    Alignment.CenterVertically
                                            ) {

                                                Column {

                                                    Text(
                                                        text = field.fieldName,
                                                        style = MaterialTheme.typography.titleSmall
                                                    )

                                                    Text(
                                                        text = field.fieldType.uppercase(),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.addFieldFromMaster(field)
                                                        Toast.makeText(
                                                            context,
                                                            "${field.fieldName} added successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                ) {

                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }


                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )


                                Button(
                                    onClick = {
                                        showCustomFieldDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                ) {

                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null
                                    )

                                    Spacer(
                                        modifier = Modifier.width(8.dp)
                                    )

                                    Text("Custom Field")
                                }

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )
                            }
                        }
                    }
//                    if (canvasFields.isNotEmpty()) {

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = "Configuration Canvas",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${canvasFields.size} fields loaded for this asset type",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (canvasFields.isEmpty()) {

                                    Text(
                                        text = "No fields added to canvas yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                } else {
                                    LazyColumn(
                                        state = lazyListState,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                        itemsIndexed(
                                            canvasFields,
                                            key = { _, item ->
                                                item.id
                                            }
                                        ) { index, field ->


                                            ReorderableItem(
                                                reorderableState,
                                                key = field.id
                                            ) {

                                                ConfigurationFieldCard(

                                                    fieldName = field.label,
                                                    onCardClick = {
                                                        editingField = field
                                                    },

                                                    dragModifier = Modifier.draggableHandle(),

                                                    onViewClick = {
                                                        selectedField = field
                                                    },

                                                    onCopyClick = {
                                                        viewModel.duplicateField(index)
                                                    },

                                                    onDeleteClick = {
                                                        viewModel.removeField(index)
                                                    }
                                                )
                                            }


                                            Spacer(
                                                modifier = Modifier.height(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
//                    }
                }
            }
            selectedField?.let { field ->

                AlertDialog(
                    onDismissRequest = {
                        selectedField = null
                    },

                    title = {
                        Text(field.label)
                    },

                    text = {

                        Column {

                            Text("Field ID : ${field.id}")
                            Text("Type : ${field.type}")
                            Text("Required : ${field.required}")
                            Text("Read Only : ${field.readOnly}")
                            Text("Multi : ${field.multi}")
                        }
                    },

                    confirmButton = {

                        TextButton(
                            onClick = {
                                selectedField = null
                            }
                        ) {
                            Text("Close")
                        }
                    }
                )
            }
            if (showCustomFieldDialog) {

                AlertDialog(
                    onDismissRequest = {
                        showCustomFieldDialog = false
                    },

                    title = {
                        Text("Custom Field")
                    },

                    text = {

                        Column {

                            Text(
                                text = "Field Master Template",
                                style = MaterialTheme.typography.labelMedium
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = {
                                    expanded = !expanded
                                }
                            ) {

                                OutlinedTextField(
                                    value = selectedTemplate?.fieldName ?: "Start Without Template",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    placeholder = {
                                        Text("Select Template")
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = expanded
                                        )
                                    }
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = {
                                        expanded = false
                                    }
                                ) {

                                    DropdownMenuItem(
                                        text = {
                                            Text("Start Without Template")
                                        },
                                        onClick = {

                                            selectedTemplate = null

                                            fieldCode = ""
                                            fieldLabel = ""
                                            selectedDataType = ""

                                            expanded = false
                                        }
                                    )

                                    formFieldMasters.forEach { field ->

                                        DropdownMenuItem(
                                            text = {
                                                Text(field.fieldName)
                                            },
                                            onClick = {

                                                selectedTemplate = field
                                                fieldCode = field.fieldCode
                                                fieldLabel = field.fieldName
                                                selectedDataType = field.fieldType
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            OutlinedTextField(
                                value = fieldCode,
                                onValueChange = {
                                    fieldCode = it
                                    fieldCodeError = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Field Code")
                                },
                                isError = fieldCodeError
                            )

                            if (fieldCodeError) {
                                Text(
                                    text = "Field Code is required",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            OutlinedTextField(
                                value = fieldLabel,
                                onValueChange = {
                                    fieldLabel = it
                                    fieldLabelError = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Field Label")
                                },
                                isError = fieldLabelError
                            )

                            if (fieldLabelError) {
                                Text(
                                    text = "Field Label is required",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "Data Type",
                                style = MaterialTheme.typography.labelMedium
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            OutlinedTextField(
                                value = selectedDataType,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                label = {
                                    Text("Data Type")
                                }
                            )
                            if (dataTypeError) {
                                Text(
                                    text = "Data Type is required",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = "Required",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Switch(
                                    checked = required,
                                    onCheckedChange = {
                                        required = it
                                    }
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    text = "Searchable",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Switch(
                                    checked = searchable,
                                    onCheckedChange = {
                                        searchable = it
                                    }
                                )
                            }
                        }
                    },

                    confirmButton = {

                        Row {

                            TextButton(
                                onClick = {
                                    showCustomFieldDialog = false
                                }
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {

                                    fieldCodeError = fieldCode.isBlank()
                                    fieldLabelError = fieldLabel.isBlank()
                                    dataTypeError = selectedDataType.isBlank()

                                    if (
                                        fieldCodeError ||
                                        fieldLabelError ||
                                        dataTypeError
                                    ) {
                                        return@Button
                                    }

                                    viewModel.addCustomField(
                                        fieldCode = fieldCode,
                                        fieldLabel = fieldLabel,
                                        dataType = selectedDataType,
                                        required = required,
                                        searchable = searchable,
                                        fieldMasterId = selectedTemplate?.fieldMasterId ?: 0
                                    )

                                    Toast.makeText(
                                        context,
                                        "Field added successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    fieldCode = ""
                                    fieldLabel = ""
                                    selectedDataType = ""
                                    selectedTemplate = null
                                    required = false
                                    searchable = false

                                    showCustomFieldDialog = false
                                }
                            ) {
                                Text("Add To Canvas")
                            }
                        }
                    }
                )
            }
            editingField?.let { field ->

                var editFieldCode by remember(field.id) { mutableStateOf(field.id) }

                var editFieldLabel by remember(field.id) { mutableStateOf(field.label) }
                var editDataType by remember(field.id) { mutableStateOf(field.type) }
                var editPlaceholder by remember(field.id) { mutableStateOf(field.placeholder ?: "") }
                var editDefaultValue by remember(field.id) { mutableStateOf(field.defaultValue?.toString() ?: "") }
                var editRequired by remember(field.id) { mutableStateOf(field.required) }
                var editSearchable by remember(field.id) { mutableStateOf(field.searchable) }
                var editReadOnly by remember(field.id) { mutableStateOf(field.readOnly) }
                var editResolveFromMaster by remember(field.id) { mutableStateOf(field.resolveFromMaster) }

                val validationObject = field.validation

                var minValue by remember(field.id) { mutableStateOf(validationObject?.get("min")?.asString ?: "") }
                var maxValue by remember(field.id) { mutableStateOf(validationObject?.get("max")?.asString ?: "") }
                var minLength by remember(field.id) { mutableStateOf(validationObject?.get("minLength")?.asString ?: "") }
                var maxLength by remember(field.id) { mutableStateOf(validationObject?.get("maxLength")?.asString ?: "") }
                var regexPattern by remember(field.id) { mutableStateOf(validationObject?.get("regex")?.asString ?: "") }
                val hasMin = validationObject?.has("min") == true
                val hasMax = validationObject?.has("max") == true
                val hasMinLength = validationObject?.has("minLength") == true
                val hasMaxLength = validationObject?.has("maxLength") == true
                val isValidationEmpty = validationObject == null || validationObject.entrySet().isEmpty()

                var optionItems by remember(field.id) {
                    mutableStateOf(
                        (field.options ?: emptyList()).map {
                            OptionItem(
                                value = it.value,
                                label = it.label
                            )
                        }.toMutableList()
                    )
                }

                val existingDependency =
                    field.dependencies.firstOrNull()

                var selectedParentField by remember(field.id) {

                    mutableStateOf<DynamicField?>(

                        canvasFields.firstOrNull {

                            it.id == existingDependency?.field
                        }
                    )
                }
                var requiredParentSelection by remember(field.id) { mutableStateOf(field.dependencies.firstOrNull()?.required ?: false) }
                var clearOnParentChange by remember(field.id) { mutableStateOf(field.dependencies.firstOrNull()?.clearOnParentChange ?: false) }

                val isAdvancedField =
                    field.type.equals("dropdown", true) ||
                            field.type.equals("checkbox", true) ||
                            field.type.equals("radio", true)

                val availableParentFields =
                    canvasFields.filter {
                        it.id != field.id
                    }

                val parentOptions =
                    selectedParentField?.options ?: emptyList()

                var editMulti by remember(field.id) {
                    mutableStateOf(field.multi)
                }

                var parentExpanded by remember(field.id) {
                    mutableStateOf(false)
                }

                var dependentOptionMap by remember(field.id) {

                    mutableStateOf(

                        mutableMapOf<String, String>().apply {

                            field.dependentOptions
                                ?.entrySet()
                                ?.firstOrNull()
                                ?.value
                                ?.asJsonObject
                                ?.entrySet()
                                ?.forEach { entry ->

                                    val values =
                                        entry.value
                                            .asJsonArray
                                            .map {
                                                it.asJsonObject["label"].asString
                                            }

                                    put(
                                        entry.key,
                                        values.joinToString(",")
                                    )
                                }
                        }
                    )
                }

                var visibilityLogic by remember(field.id) {
                    mutableStateOf(
                        field.conditionalLogic?.logic ?: "AND"
                    )
                }

                var editResetOnHide by remember(field.id) {
                    mutableStateOf(field.resetOnHide)
                }

                var visibilityConditions by remember(field.id) {
                    mutableStateOf(
                        field.conditionalLogic?.conditions?.toMutableList()
                            ?: mutableListOf<assetCondition>()
                    )
                }

                var visibilityLogicExpanded by remember(field.id) {
                    mutableStateOf(false)
                }

                val visibilityFields =
                    canvasFields.filter {
                        it.id != field.id
                    }


                Dialog(
                    onDismissRequest = {
                        editingField = null
                    }
                ) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.90f),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            // Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {

                                Text(
                                    text = "Field Configuration",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Text(
                                    text = field.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Tabs
                            ScrollableTabRow(
                                selectedTabIndex = selectedTab,
                                edgePadding = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                listOf(
                                    "General",
                                    "Validation",
                                    "Advanced",
                                    "Visibility"
                                ).forEachIndexed { index, title ->

                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = {
                                            selectedTab = index
                                        },
                                        text = {
                                            Text(title)
                                        }
                                    )
                                }
                            }

                            // Content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(
                                        rememberScrollState()
                                    )
                                    .padding(16.dp)
                            ) {

                                when (selectedTab) {

                                    0 -> {

                                        OutlinedTextField(
                                            value = editFieldCode,
                                            onValueChange = {
                                                editFieldCode = it
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text("Field Code")
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = editFieldLabel,
                                            onValueChange = {
                                                editFieldLabel = it
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text("Field Label")
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = editDataType,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text("Data Type")
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = editPlaceholder,
                                            onValueChange = {
                                                editPlaceholder = it
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text("Placeholder")
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = editDefaultValue,
                                            onValueChange = {
                                                editDefaultValue = it
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = {
                                                Text("Default Value")
                                            }
                                        )

                                        Spacer(
                                            modifier = Modifier.height(16.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Required")

                                            Switch(
                                                checked = editRequired,
                                                onCheckedChange = {
                                                    editRequired = it
                                                }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Searchable")

                                            Switch(
                                                checked = editSearchable,
                                                onCheckedChange = {
                                                    editSearchable = it
                                                }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Read Only")

                                            Switch(
                                                checked = editReadOnly,
                                                onCheckedChange = {
                                                    editReadOnly = it
                                                }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Resolve From Master")

                                            Switch(
                                                checked = editResolveFromMaster,
                                                onCheckedChange = {
                                                    editResolveFromMaster = it
                                                }
                                            )
                                        }
                                    }

                                    1 -> {

                                        Column {

                                            if (hasMin || hasMax) {

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(12.dp)
                                                ) {

                                                    OutlinedTextField(
                                                        value = minValue,
                                                        onValueChange = {
                                                            minValue = it
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        label = {
                                                            Text("Min")
                                                        }
                                                    )

                                                    OutlinedTextField(
                                                        value = maxValue,
                                                        onValueChange = {
                                                            maxValue = it
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        label = {
                                                            Text("Max")
                                                        }
                                                    )
                                                }

                                                Spacer(
                                                    modifier = Modifier.height(12.dp)
                                                )
                                            }

                                            if (hasMinLength || hasMaxLength) {

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(12.dp)
                                                ) {

                                                    OutlinedTextField(
                                                        value = minLength,
                                                        onValueChange = {
                                                            minLength = it
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        label = {
                                                            Text("Min Length")
                                                        }
                                                    )

                                                    OutlinedTextField(
                                                        value = maxLength,
                                                        onValueChange = {
                                                            maxLength = it
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        label = {
                                                            Text("Max Length")
                                                        }
                                                    )
                                                }

                                                Spacer(
                                                    modifier = Modifier.height(12.dp)
                                                )
                                            }

                                            OutlinedTextField(
                                                value = regexPattern,
                                                onValueChange = {
                                                    regexPattern = it
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                label = {
                                                    Text("Regex")
                                                }
                                            )

                                            if (isValidationEmpty) {

                                                Spacer(
                                                    modifier = Modifier.height(12.dp)
                                                )

                                                Card(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {

                                                    Text(
                                                        text = "Regex validation is available for this type.",
                                                        modifier = Modifier.padding(16.dp),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    2 -> {

                                        if (!isAdvancedField) {

                                            Text(
                                                text = "No advanced settings available for this field type."
                                            )

                                        } else {

                                            Column {

                                                if (
                                                    field.type.equals(
                                                        "dropdown",
                                                        true
                                                    )
                                                ) {

                                                    Spacer(
                                                        modifier = Modifier.height(12.dp)
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                            Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                            Alignment.CenterVertically
                                                    ) {

                                                        Text("Allow Multiple Selection")

                                                        Switch(
                                                            checked = editMulti,
                                                            onCheckedChange = {
                                                                editMulti = it
                                                            }
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement =
                                                        Arrangement.SpaceBetween,
                                                    verticalAlignment =
                                                        Alignment.CenterVertically
                                                ) {

                                                    Text(
                                                        text = "Options",
                                                        style = MaterialTheme.typography.titleMedium
                                                    )

                                                    TextButton(
                                                        onClick = {

                                                            optionItems =
                                                                optionItems.toMutableList().apply {

                                                                    add(
                                                                        OptionItem()
                                                                    )
                                                                }
                                                        }
                                                    ) {
                                                        Text("Add")
                                                    }
                                                }

                                                Spacer(
                                                    modifier = Modifier.height(8.dp)
                                                )

                                                optionItems.forEachIndexed { index, option ->

                                                    Card(
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {

                                                        Column(
                                                            modifier = Modifier.padding(12.dp)
                                                        ) {

                                                            OutlinedTextField(
                                                                value = option.value,
                                                                onValueChange = { value ->

                                                                    optionItems =
                                                                        optionItems.toMutableList().apply {

                                                                            this[index] =
                                                                                this[index].copy(
                                                                                    value = value
                                                                                )
                                                                        }
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                label = {
                                                                    Text("Value")
                                                                }
                                                            )

                                                            Spacer(
                                                                modifier = Modifier.height(8.dp)
                                                            )

                                                            OutlinedTextField(
                                                                value = option.label,
                                                                onValueChange = { label ->

                                                                    optionItems =
                                                                        optionItems.toMutableList().apply {

                                                                            this[index] =
                                                                                this[index].copy(
                                                                                    label = label
                                                                                )
                                                                        }
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                label = {
                                                                    Text("Label")
                                                                }
                                                            )

                                                            Spacer(
                                                                modifier = Modifier.height(8.dp)
                                                            )

                                                            TextButton(
                                                                onClick = {

                                                                    optionItems =
                                                                        optionItems.toMutableList().apply {

                                                                            removeAt(index)
                                                                        }
                                                                }
                                                            ) {
                                                                Text("Delete")
                                                            }
                                                        }
                                                    }

                                                    Spacer(
                                                        modifier = Modifier.height(8.dp)
                                                    )
                                                }

                                                Spacer(
                                                    modifier = Modifier.height(16.dp)
                                                )

                                                Text(
                                                    text = "Dependent Field",
                                                    style = MaterialTheme.typography.titleMedium
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(8.dp)
                                                )

                                                ExposedDropdownMenuBox(
                                                    expanded = parentExpanded,
                                                    onExpandedChange = {
                                                        parentExpanded = !parentExpanded
                                                    }
                                                ) {

                                                    OutlinedTextField(
                                                        value = selectedParentField?.label ?: "",
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        modifier = Modifier
                                                            .menuAnchor()
                                                            .fillMaxWidth(),
                                                        placeholder = {
                                                            Text("Select Parent Field")
                                                        },
                                                        trailingIcon = {
                                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                                expanded = parentExpanded
                                                            )
                                                        }
                                                    )

                                                    ExposedDropdownMenu(
                                                        expanded = parentExpanded,
                                                        onDismissRequest = {
                                                            parentExpanded = false
                                                        }
                                                    ) {

                                                        availableParentFields.forEach { parent ->

                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(parent.label)
                                                                },
                                                                onClick = {

                                                                    selectedParentField = parent
                                                                    parentExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                if (selectedParentField != null) {

                                                    Spacer(
                                                        modifier = Modifier.height(12.dp)
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {

                                                        Text("Required Parent Selection")

                                                        Switch(
                                                            checked = requiredParentSelection,
                                                            onCheckedChange = {
                                                                requiredParentSelection = it
                                                            }
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {

                                                        Text("Clear On Parent Change")

                                                        Switch(
                                                            checked = clearOnParentChange,
                                                            onCheckedChange = {
                                                                clearOnParentChange = it
                                                            }
                                                        )
                                                    }

                                                    Spacer(
                                                        modifier = Modifier.height(12.dp)
                                                    )

                                                    if (parentOptions.isEmpty()) {

                                                        Text(
                                                            text = "Add options to parent field first.",
                                                            color = MaterialTheme.colorScheme.error
                                                        )

                                                    } else {

                                                        Text(
                                                            text = "Dependent Option Mapping",
                                                            style = MaterialTheme.typography.titleSmall
                                                        )

                                                        parentOptions.forEach { option ->

                                                            Spacer(
                                                                modifier = Modifier.height(12.dp)
                                                            )

                                                            Text(
                                                                text = option.label,
                                                                style = MaterialTheme.typography.labelLarge
                                                            )

                                                            Spacer(
                                                                modifier = Modifier.height(4.dp)
                                                            )

                                                            OutlinedTextField(
                                                                value = dependentOptionMap[option.value] ?: "",
                                                                onValueChange = { value ->

                                                                    dependentOptionMap =
                                                                        dependentOptionMap.toMutableMap().apply {

                                                                            put(
                                                                                option.value,
                                                                                value
                                                                            )
                                                                        }
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                placeholder = {
                                                                    Text(
                                                                        "Enter comma separated options"
                                                                    )
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    3 -> {

                                        Column {

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement =
                                                    Arrangement.SpaceBetween,
                                                verticalAlignment =
                                                    Alignment.CenterVertically
                                            ) {

                                                Text(
                                                    text = "Conditional Visibility",
                                                    style = MaterialTheme.typography.titleMedium
                                                )

                                                TextButton(
                                                    onClick = {

                                                        visibilityConditions =
                                                            visibilityConditions.toMutableList().apply {

                                                                add(
                                                                    assetCondition(
                                                                        field = "",
                                                                        value = "",
                                                                        operator = "equals"
                                                                    )
                                                                )
                                                            }
                                                    }
                                                ) {
                                                    Text("Add Rule")
                                                }
                                            }

                                            Spacer(
                                                modifier = Modifier.height(12.dp)
                                            )

                                            ExposedDropdownMenuBox(
                                                expanded = visibilityLogicExpanded,
                                                onExpandedChange = {
                                                    visibilityLogicExpanded =
                                                        !visibilityLogicExpanded
                                                }
                                            ) {

                                                OutlinedTextField(
                                                    value = visibilityLogic,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    modifier = Modifier
                                                        .menuAnchor()
                                                        .fillMaxWidth(),
                                                    label = {
                                                        Text("Logic")
                                                    },
                                                    trailingIcon = {
                                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                            expanded = visibilityLogicExpanded
                                                        )
                                                    }
                                                )

                                                ExposedDropdownMenu(
                                                    expanded = visibilityLogicExpanded,
                                                    onDismissRequest = {
                                                        visibilityLogicExpanded = false
                                                    }
                                                ) {

                                                    listOf(
                                                        "AND",
                                                        "OR"
                                                    ).forEach { logic ->

                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(logic)
                                                            },
                                                            onClick = {

                                                                visibilityLogic = logic
                                                                visibilityLogicExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(
                                                modifier = Modifier.height(12.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement =
                                                    Arrangement.SpaceBetween,
                                                verticalAlignment =
                                                    Alignment.CenterVertically
                                            ) {

                                                Text("Reset Value When Hidden")

                                                Switch(
                                                    checked = editResetOnHide,
                                                    onCheckedChange = {
                                                        editResetOnHide = it
                                                    }
                                                )
                                            }

                                            Spacer(
                                                modifier = Modifier.height(12.dp)
                                            )

                                            if (visibilityConditions.isEmpty()) {

                                                Text(
                                                    text = "No rules. Field stays visible.",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            else {

                                                visibilityConditions.forEachIndexed { index, condition ->

                                                    var fieldExpanded by remember(index) { mutableStateOf(false)}

                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                    ) {

                                                        Column(
                                                            modifier = Modifier.padding(12.dp)
                                                        ) {

                                                            Text(
                                                                text = "Rule ${index + 1}",
                                                                style = MaterialTheme.typography.titleSmall
                                                            )

                                                            Spacer(
                                                                modifier = Modifier.height(8.dp)
                                                            )

                                                            Text("Controlling Field")

                                                            Spacer(
                                                                modifier = Modifier.height(4.dp)
                                                            )

                                                            ExposedDropdownMenuBox(
                                                                expanded = fieldExpanded,
                                                                onExpandedChange = {
                                                                    fieldExpanded = !fieldExpanded
                                                                }
                                                            ) {

                                                                OutlinedTextField(
                                                                    value =
                                                                        visibilityFields.firstOrNull {
                                                                            it.id == condition.field
                                                                        }?.label ?: "",
                                                                    onValueChange = {},
                                                                    readOnly = true,
                                                                    modifier = Modifier
                                                                        .menuAnchor()
                                                                        .fillMaxWidth(),
                                                                    placeholder = {
                                                                        Text("Select Field")
                                                                    },
                                                                    trailingIcon = {
                                                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                                            expanded = fieldExpanded
                                                                        )
                                                                    }
                                                                )

                                                                ExposedDropdownMenu(
                                                                    expanded = fieldExpanded,
                                                                    onDismissRequest = {
                                                                        fieldExpanded = false
                                                                    }
                                                                ) {

                                                                    visibilityFields.forEach { canvasField ->

                                                                        DropdownMenuItem(
                                                                            text = {
                                                                                Text(canvasField.label)
                                                                            },
                                                                            onClick = {

                                                                                visibilityConditions =
                                                                                    visibilityConditions.toMutableList().apply {

                                                                                        this[index] =
                                                                                            condition.copy(
                                                                                                field = canvasField.id
                                                                                            )
                                                                                    }

                                                                                fieldExpanded = false
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Spacer(
                                                                modifier = Modifier.height(8.dp)
                                                            )

                                                            Text("Operator")

                                                            Spacer(
                                                                modifier = Modifier.height(4.dp)
                                                            )

                                                            var operatorExpanded by remember(index) {
                                                                mutableStateOf(false)
                                                            }

                                                            ExposedDropdownMenuBox(
                                                                expanded = operatorExpanded,
                                                                onExpandedChange = {
                                                                    operatorExpanded = !operatorExpanded
                                                                }
                                                            ) {

                                                                OutlinedTextField(
                                                                    value = condition.operator,
                                                                    onValueChange = {},
                                                                    readOnly = true,
                                                                    modifier = Modifier
                                                                        .menuAnchor()
                                                                        .fillMaxWidth(),
                                                                    placeholder = {
                                                                        Text("Select Operator")
                                                                    },
                                                                    trailingIcon = {
                                                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                                            expanded = operatorExpanded
                                                                        )
                                                                    }
                                                                )

                                                                ExposedDropdownMenu(
                                                                    expanded = operatorExpanded,
                                                                    onDismissRequest = {
                                                                        operatorExpanded = false
                                                                    }
                                                                ) {

                                                                    listOf(
                                                                        "Equals",
                                                                        "Not Equals",
                                                                        "Contains"
                                                                    ).forEach { operator ->

                                                                        DropdownMenuItem(
                                                                            text = {
                                                                                Text(operator)
                                                                            },
                                                                            onClick = {

                                                                                visibilityConditions =
                                                                                    visibilityConditions.toMutableList().apply {

                                                                                        this[index] =
                                                                                            condition.copy(
                                                                                                operator = operator
                                                                                            )
                                                                                    }

                                                                                operatorExpanded = false
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Spacer(
                                                                modifier = Modifier.height(8.dp)
                                                            )

                                                            OutlinedTextField(
                                                                value = condition.value,
                                                                onValueChange = { newValue ->

                                                                    visibilityConditions =
                                                                        visibilityConditions.toMutableList().apply {

                                                                            this[index] =
                                                                                condition.copy(
                                                                                    value = newValue
                                                                                )
                                                                        }
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                label = {
                                                                    Text("Comparison Value")
                                                                }
                                                            )

                                                            Spacer(
                                                                modifier = Modifier.height(8.dp)
                                                            )

                                                            TextButton(
                                                                onClick = {

                                                                    visibilityConditions =
                                                                        visibilityConditions.toMutableList().apply {

                                                                            removeAt(index)
                                                                        }
                                                                }
                                                            ) {
                                                                Text("Delete Rule")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Bottom Buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.End
                            ) {

                                TextButton(
                                    onClick = {
                                        editingField = null
                                    }
                                ) {
                                    Text("Cancel")
                                }

                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )

                                Button(
                                    onClick = {

                                        val validationJson =
                                            JsonObject().apply {

                                                if (minValue.isNotBlank()) {
                                                    addProperty("min", minValue)
                                                }

                                                if (maxValue.isNotBlank()) {
                                                    addProperty("max", maxValue)
                                                }

                                                if (minLength.isNotBlank()) {
                                                    addProperty("minLength", minLength)
                                                }

                                                if (maxLength.isNotBlank()) {
                                                    addProperty("maxLength", maxLength)
                                                }

                                                if (regexPattern.isNotBlank()) {
                                                    addProperty("regex", regexPattern)
                                                }
                                            }

                                        val dependencyList =
                                            if (selectedParentField != null) {

                                                listOf(
                                                    FieldsDependency(
                                                        field = selectedParentField!!.id,
                                                        required = requiredParentSelection,
                                                        clearOnParentChange = clearOnParentChange
                                                    )
                                                )

                                            } else {
                                                emptyList()
                                            }

                                        val dependentOptionsJson = JsonObject()

                                        if (
                                            selectedParentField != null &&
                                            dependentOptionMap.isNotEmpty()
                                        ) {

                                            val parentObject = JsonObject()

                                            dependentOptionMap.forEach { (parentValue, childValues) ->

                                                val optionArray = JsonArray()

                                                childValues
                                                    .split(",")
                                                    .map { it.trim() }
                                                    .filter { it.isNotEmpty() }
                                                    .forEach { value ->

                                                        val optionObj = JsonObject()

                                                        optionObj.addProperty(
                                                            "label",
                                                            value
                                                        )

                                                        optionObj.addProperty(
                                                            "value",
                                                            value
                                                        )

                                                        optionArray.add(optionObj)
                                                    }

                                                parentObject.add(
                                                    parentValue,
                                                    optionArray
                                                )
                                            }

                                            dependentOptionsJson.add(
                                                selectedParentField!!.id,
                                                parentObject
                                            )
                                        }

                                        val updatedField = field.copy(

                                            // General
                                            id = editFieldCode,
                                            label = editFieldLabel,
                                            type = editDataType,
                                            placeholder = editPlaceholder,
                                            defaultValue = editDefaultValue,
                                            required = editRequired,
                                            searchable = editSearchable,
                                            readOnly = editReadOnly,
                                            resolveFromMaster = editResolveFromMaster,

                                            // Validation
                                            validation =
                                                if (validationJson.entrySet().isEmpty())
                                                    null
                                                else
                                                    validationJson,

                                            // Advanced
                                            multi = editMulti,

                                            options =
                                                optionItems.map {

                                                    FieldOption(
                                                        value = it.value,
                                                        label = it.label
                                                    )
                                                },

                                            dependencies = dependencyList,

                                            dependentOptions =
                                                if (dependentOptionsJson.entrySet().isEmpty())
                                                    null
                                                else
                                                    dependentOptionsJson,

                                            // Visibility
                                            resetOnHide = editResetOnHide,

                                            conditionalLogic =
                                                if (visibilityConditions.isEmpty()) {
                                                    null
                                                } else {
                                                    assetConditionalLogic(
                                                        logic = visibilityLogic,
                                                        conditions = visibilityConditions
                                                    )
                                                }
                                        )

                                        viewModel.updateField(updatedField)

                                        Toast.makeText(
                                            context,
                                            "Field updated successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        editingField = null
                                    }
                                ) {
                                    Text("Save Field Changes")
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun InfoTile(
    title: String,
    value: String,
    icon: ImageVector? = null,
    imageUrl: String? = null
) {

    Card(
        modifier = Modifier.width(220.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (!imageUrl.isNullOrEmpty()) {

                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )

            } else {

                Icon(
                    imageVector = icon!!,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun ConfigurationFieldCard(
    fieldName: String,
    dragModifier: Modifier = Modifier,
    onCardClick: () -> Unit = {},
    onViewClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCardClick()
            },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag",
                modifier = dragModifier
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = fieldName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = onViewClick
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "View"
                )
            }

            IconButton(
                onClick = onCopyClick
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy"
                )
            }

            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}