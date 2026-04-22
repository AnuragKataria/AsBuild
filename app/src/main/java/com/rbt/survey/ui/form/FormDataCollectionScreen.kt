package com.rbt.survey.ui.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import com.rbt.survey.data.model.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.location.Location
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDataCollectionScreen(
    viewModel: FormDataCollectionViewModel,
    onBack: () -> Unit,
    onNavigateToMap: (String, String, String) -> Unit,
    navController: androidx.navigation.NavController,
    onSubmitSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val submitSuccess by viewModel.submitSuccess.collectAsState()

    val allDisplayFields = remember(uiState) {
        val successState = uiState as? FormUiState.Success ?: return@remember emptyList()
        successState.formData.currentVersion?.schema?.fields ?: emptyList()
    }

    // Observe result from Map screen
    val mapResult by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Pair<String, String>>("map_result")
        ?.observeAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(mapResult) {
        mapResult?.let { (fieldId, value) ->
            viewModel.onFieldValueChange(fieldId, value, allDisplayFields)
            navController.currentBackStackEntry?.savedStateHandle?.remove<Pair<String, String>>("map_result")
        }
    }

    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            onSubmitSuccess()
        }
    }

    // Store field-level error messages
    val fieldErrors = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val title = (uiState as? FormUiState.Success)?.formData?.formName ?: "Form Collection"
                    Text(title, fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is FormUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is FormUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val visibleFields = allDisplayFields.filter { field ->
                                field.type != "hidden" && viewModel.shouldShowField(field)
                            }

                            items(visibleFields) { field ->
                                DynamicField(
                                    field = field, 
                                    allFields = allDisplayFields, 
                                    viewModel = viewModel,
                                    onNavigateToMap = onNavigateToMap,
                                    error = fieldErrors[field.id]
                                )
                            }
                        }

                        val context = LocalContext.current
                        Button(
                            onClick = { 
                                // Clear previous errors
                                fieldErrors.clear()
                                
                                // Validate all required fields
                                val invalidFields = allDisplayFields.filter { field ->

                                    val isVisible = viewModel.shouldShowField(field)
                                    if (!isVisible) return@filter false
                                    if (!field.required) return@filter false
                                    
                                    val value = viewModel.fieldValues[field.id]
                                    val isMissing = when {
                                        value == null -> true
                                        value is String && value.isBlank() -> true
                                        value is List<*> && value.isEmpty() -> true
                                        else -> false
                                    }
                                    
                                    if (isMissing) {
                                        fieldErrors[field.id] = "${field.label} is required"
                                    }
                                    isMissing
                                }
                                
                                if (invalidFields.isEmpty()) {
                                    viewModel.submitForm(context, allDisplayFields) 
                                } else {
                                    android.widget.Toast.makeText(context, "Please fix errors in the form", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Submit Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is FormUiState.Error -> {
                    Text(
                        (uiState as FormUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicField(
    field: FormField,
    allFields: List<FormField>,
    viewModel: FormDataCollectionViewModel,
    onNavigateToMap: (String, String, String) -> Unit,
    error: String? = null
) {
    val currentValue = viewModel.fieldValues[field.id]
    val options = viewModel.getOptionsForField(field)
    val isReadOnly = field.readOnly == true

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = field.label + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelLarge,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (field.type) {
            "dropdown" -> {
                if (field.multi == true) {
                    MultiSelectDropdown(field, options, currentValue as? List<String> ?: emptyList(), viewModel, allFields, error)
                } else {
                    SingleSelectDropdown(field, options, currentValue?.toString() ?: "", viewModel, allFields, isReadOnly, error)
                }
            }
            "checkbox" -> {
                Column {
                    MultiCheckboxField(field, options, currentValue as? List<String> ?: emptyList(), viewModel, allFields, isReadOnly)
                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }
            }
            "text", "textarea", "computed", "currency", "email", "integer" -> {
                val value = currentValue?.toString() ?: ""
                val validationError = remember(value) { getValidationError(field, value) }
                val displayError = error ?: validationError
                
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (!isReadOnly) viewModel.onFieldValueChange(field.id, it, allFields) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(field.label) },
                    placeholder = { Text(field.placeholder ?: "Enter ${field.label}") },
                    shape = MaterialTheme.shapes.medium,
                    readOnly = isReadOnly,
                    isError = displayError != null,
                    supportingText = { if (displayError != null) Text(displayError) },
                    singleLine = field.type != "textarea",
                    minLines = if (field.type == "textarea") 3 else 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = when(field.type) {
                            "currency", "integer" -> KeyboardType.Number
                            "email" -> KeyboardType.Email
                            else -> KeyboardType.Text
                        }
                    ),
                    leadingIcon = if (field.type == "currency") { { Text("₹") } } else null,
                    trailingIcon = if (error != null) { { Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error) } } else null
                )
            }
            "date" -> {
                DatePickerField(field, currentValue?.toString() ?: "", viewModel, allFields, isReadOnly, error)
            }
            "datetime" -> {
                DateTimePickerField(field, currentValue?.toString() ?: "", viewModel, allFields, isReadOnly, error)
            }
            "time" -> {
                TimePickerField(field, currentValue?.toString() ?: "", viewModel, allFields, isReadOnly, error)
            }
            "daterange" -> {
                DateRangePickerField(field, currentValue?.toString() ?: "", viewModel, allFields, isReadOnly, error)
            }
            "slider" -> {
                val value = (currentValue as? Number)?.toFloat() ?: field.validation?.min?.toFloat() ?: 0f
                val range = (field.validation?.min?.toFloat() ?: 0f)..(field.validation?.max?.toFloat() ?: 100f)
                Column {
                    Slider(
                        value = value,
                        onValueChange = { if (!isReadOnly) viewModel.onFieldValueChange(field.id, it, allFields) },
                        valueRange = range,
                        enabled = !isReadOnly
                    )
                    Text(value.toInt().toString(), style = MaterialTheme.typography.bodySmall)
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            "rating" -> {
                val rating = (currentValue as? Number)?.toInt() ?: 0
                val maxRating = field.validation?.max?.toInt() ?: 5
                Column {
                    Row {
                        repeat(maxRating) { index ->
                            IconButton(onClick = { if (!isReadOnly) viewModel.onFieldValueChange(field.id, index + 1, allFields) }) {
                                Icon(
                                    imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = null,
                                    tint = if (index < rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    if (error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            "color" -> {
                var showColorPicker by remember { mutableStateOf(false) }
                if (showColorPicker) {
                    ColorPickerDialog(
                        onDismiss = { showColorPicker = false },
                        onColorSelected = { color ->
                            viewModel.onFieldValueChange(field.id, color, allFields)
                            showColorPicker = false
                        }
                    )
                }

                OutlinedTextField(
                    value = currentValue?.toString() ?: "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth().clickable { if (!isReadOnly) showColorPicker = true },
                    placeholder = { Text("Select Color") },
                    shape = MaterialTheme.shapes.medium,
                    readOnly = true,
                    enabled = !isReadOnly,
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 8.dp)
                                .background(
                                    color = try {
                                        Color(android.graphics.Color.parseColor(currentValue?.toString() ?: "#000000"))
                                    } catch (e: Exception) {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .clickable { if (!isReadOnly) showColorPicker = true }
                        )
                    }
                )
            }
            "tags" -> {
                TagsField(field, currentValue as? List<String> ?: emptyList(), viewModel, allFields, isReadOnly, error)
            }
            "image", "video", "file", "document" -> {
                FileUploadField(field, currentValue?.toString() ?: "", viewModel, allFields, isReadOnly, error)
            }
            "map" -> {
                val mapType = when {
                    field.geometryType?.any { it.uppercase().contains("POLYGON") } == true -> "Polygon"
                    field.geometryType?.any { it.uppercase().contains("LINE") } == true -> "Polyline"
                    else -> "Point"
                }
                if (isReadOnly) {
                    // Read-only display — completely non-interactive
                    if (mapType == "Point") {
                        // Geo Point readonly: resolve and show address from coordinates
                        val context = LocalContext.current
                        val coordString = currentValue?.toString() ?: ""
                        var resolvedAddress by remember(coordString) { mutableStateOf(coordString) }
                        val scope = rememberCoroutineScope()

                        LaunchedEffect(coordString) {
                            if (coordString.isNotBlank()) {
                                scope.launch {
                                    val address = withContext(Dispatchers.IO) {
                                        resolveAddressFromCoordinates(context, coordString)
                                    }
                                    if (address != null) resolvedAddress = address
                                }
                            }
                        }

                        OutlinedTextField(
                            value = resolvedAddress,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Location (Read-only)") },
                            shape = MaterialTheme.shapes.medium,
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        )
                    } else {
                        // Geo Linestring / Polygon readonly: plain non-clickable info card
                        val icon = if (mapType == "Polygon") Icons.Default.Polyline else Icons.Default.Timeline
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (currentValue?.toString().isNullOrBlank())
                                        "No $mapType data"
                                    else
                                        currentValue.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    val coordString = currentValue?.toString() ?: ""
                    val context = LocalContext.current
                    var resolvedAddress by remember(coordString) { mutableStateOf(coordString) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(coordString) {
                        if (coordString.isNotBlank()) {
                            scope.launch {
                                val address = withContext(Dispatchers.IO) {
                                    resolveAddressFromCoordinates(context, coordString)
                                }
                                if (address != null) resolvedAddress = address
                            }
                        }
                    }
                    // Editable state — tappable
                    OutlinedTextField(
                        value = resolvedAddress,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable {
                            onNavigateToMap(mapType, field.id, currentValue?.toString() ?: "")
                        },
                        placeholder = { Text("Tap to select $mapType") },
                        shape = MaterialTheme.shapes.medium,
                        readOnly = true,
                        enabled = true,
                        isError = error != null,
                        supportingText = { if (error != null) Text(error) },
                        trailingIcon = {
                            IconButton(onClick = {
                                onNavigateToMap(mapType, field.id, currentValue?.toString() ?: "")
                            }) {
                                val icon = when(mapType) {
                                    "Polygon" -> Icons.Default.Polyline
                                    "Polyline" -> Icons.Default.Timeline
                                    else -> Icons.Default.LocationOn
                                }
                                Icon(icon, null)
                            }
                        }
                    )
                }
            }
            else -> {
                Text("Unsupported field type: ${field.type}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleSelectDropdown(
    field: FormField,
    options: List<FormOption>,
    currentValue: String,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.value == currentValue }
    val displayValue = selectedOption?.label ?: currentValue
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (!isReadOnly) expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(field.placeholder ?: "Select ${field.label}") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium,
            isError = error != null,
            supportingText = { if (error != null) Text(error) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("No options available") }, onClick = { }, enabled = false)
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            viewModel.onFieldValueChange(field.id, option.value, allFields)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    field: FormField,
    options: List<FormOption>,
    currentValues: List<String>,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = currentValues.map { valId -> 
        options.find { it.value == valId }?.label ?: valId 
    }.joinToString(", ")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(field.placeholder ?: "Select ${field.label}") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium,
            isError = error != null,
            supportingText = { if (error != null) Text(error) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val isSelected = currentValues.contains(option.value)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(option.label, modifier = Modifier.padding(start = 8.dp))
                        }
                    },
                    onClick = {
                        val newValues = if (isSelected) currentValues - option.value else currentValues + option.value
                        viewModel.onFieldValueChange(field.id, newValues, allFields)
                    }
                )
            }
        }
    }
}

@Composable
fun MultiCheckboxField(
    field: FormField,
    options: List<FormOption>,
    currentValues: List<String>,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean
) {
    Column {
        options.forEach { option ->
            val isSelected = currentValues.contains(option.value)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isReadOnly) {
                    val newValues = if (isSelected) currentValues - option.value else currentValues + option.value
                    viewModel.onFieldValueChange(field.id, newValues, allFields)
                }
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        if (!isReadOnly) {
                            val newValues = if (checked) currentValues + option.value else currentValues - option.value
                            viewModel.onFieldValueChange(field.id, newValues, allFields)
                        }
                    },
                    enabled = !isReadOnly
                )
                Text(option.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    field: FormField,
    currentValue: String,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean,
    error: String? = null
) {
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Date(it)
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.onFieldValueChange(field.id, format.format(date), allFields)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    OutlinedTextField(
        value = currentValue,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(field.placeholder ?: "Select Date") },
        shape = MaterialTheme.shapes.medium,
        enabled = !isReadOnly,
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }, enabled = !isReadOnly) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    field: FormField,
    currentValue: String,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean,
    error: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    viewModel.onFieldValueChange(field.id, format.format(calendar.time), allFields)
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    OutlinedTextField(
        value = currentValue,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(field.placeholder ?: "Select Date & Time") },
        shape = MaterialTheme.shapes.medium,
        enabled = !isReadOnly,
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }, enabled = !isReadOnly) {
                Icon(Icons.Default.Event, null)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    field: FormField,
    currentValue: String,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean,
    error: String? = null
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    viewModel.onFieldValueChange(field.id, time, allFields)
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    OutlinedTextField(
        value = currentValue,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(field.placeholder ?: "Select Time") },
        shape = MaterialTheme.shapes.medium,
        enabled = !isReadOnly,
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        trailingIcon = {
            IconButton(onClick = { showTimePicker = true }, enabled = !isReadOnly) {
                Icon(Icons.Default.AccessTime, null)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerField(
    field: FormField,
    currentValue: String,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean,
    error: String? = null
) {
    var showRangePicker by remember { mutableStateOf(false) }
    val rangePickerState = rememberDateRangePickerState()

    if (showRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = rangePickerState.selectedStartDateMillis
                    val end = rangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val rangeText = "${formatter.format(Date(start))} - ${formatter.format(Date(end))}"
                        viewModel.onFieldValueChange(field.id, rangeText, allFields)
                    }
                    showRangePicker = false
                }) { Text("OK") }
            }
        ) { DateRangePicker(state = rangePickerState, modifier = Modifier.height(400.dp)) }
    }

    OutlinedTextField(
        value = currentValue,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(field.placeholder ?: "Select Date Range") },
        shape = MaterialTheme.shapes.medium,
        enabled = !isReadOnly,
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        trailingIcon = {
            IconButton(onClick = { showRangePicker = true }, enabled = !isReadOnly) {
                Icon(Icons.Default.DateRange, null)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsField(
    field: FormField,
    tags: List<String>,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean,
    error: String? = null
) {
    var text by remember { mutableStateOf("") }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add tag...") },
                enabled = !isReadOnly,
                singleLine = true,
                isError = error != null,
                supportingText = { if (error != null) Text(error) }
            )
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    viewModel.onFieldValueChange(field.id, tags + text.trim(), allFields)
                    text = ""
                }
            }, enabled = !isReadOnly && text.isNotBlank()) {
                Icon(Icons.Default.Add, null)
            }
        }
        FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = { },
                    label = { Text(tag) },
                    trailingIcon = {
                        if (!isReadOnly) {
                            Icon(
                                Icons.Default.Close,
                                null,
                                modifier = Modifier.size(16.dp).clickable {
                                    viewModel.onFieldValueChange(field.id, tags - tag, allFields)
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileUploadField(
    field: FormField,
    currentValue: String,
    viewModel: FormDataCollectionViewModel,
    allFields: List<FormField>,
    isReadOnly: Boolean,
    error: String? = null
) {
    val context = LocalContext.current
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onFieldValueChange(field.id, it.toString(), allFields, context) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempUri?.let { uri ->
                watermarkImage(context, uri) { watermarkedUri ->
                    viewModel.onFieldValueChange(field.id, watermarkedUri.toString(), allFields, context)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (currentValue.isNotEmpty()) {
            val displayValue = remember(currentValue) {
                if (currentValue.startsWith("{")) {
                    try {
                        val obj = org.json.JSONObject(currentValue)
                        val path = obj.optString("relativePath")
                        if (path.isEmpty()) currentValue 
                        else if (path.startsWith("/")) "https://webgis.rbt-ltd.com$path"
                        else "https://webgis.rbt-ltd.com/$path"
                    } catch (e: Exception) {
                        currentValue
                    }
                } else {
                    currentValue
                }
            }

            if (field.type == "image") {
                AsyncImage(
                    model = displayValue,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            } else {
                val fileName = if (currentValue.startsWith("{")) {
                    try {
                        org.json.JSONObject(currentValue).optString("originalFileName")
                    } catch (e: Exception) {
                        currentValue.substringAfterLast("/")
                    }
                } else {
                    currentValue.substringAfterLast("/")
                }

                ListItem(
                    headlineContent = { Text(fileName) },
                    leadingContent = { 
                        Icon(
                            when(field.type) {
                                "video" -> Icons.Default.VideoLibrary
                                "document" -> Icons.Default.Description
                                else -> Icons.Default.AttachFile
                            },
                            null
                        )
                    },
                    modifier = Modifier.padding(vertical = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (field.type != "image") {
                OutlinedButton(
                    onClick = { launcher.launch(when(field.type){ "video" -> "video/*" "document" -> "application/pdf" else -> "*/*" }) },
                    modifier = Modifier.weight(1f),
                    enabled = !isReadOnly
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload")
                }
            }
            
            if (field.type == "image") {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        val uri = createTempImageUri(context)
                        tempUri = uri
                        cameraLauncher.launch(uri)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        )
                        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val uri = createTempImageUri(context)
                            tempUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isReadOnly
                ) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", 
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", 
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39", 
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", 
        "#795548", "#9E9E9E", "#607D8B", "#000000"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colors) { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                            .clickable { onColorSelected(colorHex) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun createTempImageUri(context: android.content.Context): android.net.Uri {
    val tempFile = java.io.File.createTempFile("temp_image_", ".jpg", context.externalCacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
fun getValidationError(field: FormField, value: String): String? {
    if (field.required && value.isEmpty()) {
        return "${field.label} is required"
    }
    if (value.isEmpty()) return null

    val validation = field.validation ?: return null
    if (validation.pattern != null && !Regex(validation.pattern).matches(value)) {
        return "Invalid format"
    }
    if (field.type == "email" && !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
        return "Invalid email address"
    }
    if (field.type == "integer" || field.type == "currency") {
        val num = value.toDoubleOrNull()
        if (num == null) return "Invalid number"
        if (field.type == "integer" && value.contains(".")) return "Integers only"
        if (validation.min != null && num < validation.min) return "Min value is ${validation.min}"
        if (validation.max != null && num > validation.max) return "Max value is ${validation.max}"
    }
    if (validation.minLength != null && value.length < validation.minLength) return "Min length is ${validation.minLength}"
    if (validation.maxLength != null && value.length > validation.maxLength) return "Max length is ${validation.maxLength}"
    
    return null
}

private fun watermarkImage(context: android.content.Context, uri: android.net.Uri, onComplete: (android.net.Uri) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context, 
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (hasLocationPermission) {
        try {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                val location = if (task.isSuccessful) task.result else null
                applyWatermark(context, uri, location, onComplete)
            }
        } catch (e: SecurityException) {
            applyWatermark(context, uri, null, onComplete)
        }
    } else {
        applyWatermark(context, uri, null, onComplete)
    }
}

private fun applyWatermark(
    context: android.content.Context, 
    uri: android.net.Uri, 
    location: Location?, 
    onComplete: (android.net.Uri) -> Unit
) {
    try {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val coord = location?.let { "Lat: ${String.format("%.6f", it.latitude)}, Lon: ${String.format("%.6f", it.longitude)}" } ?: "Location: N/A"
        
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }

        if (originalBitmap == null) {
            onComplete(uri)
            return
        }

        // Handle possible rotation from Exif
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        
        val paint = Paint().apply {
            color = AndroidColor.YELLOW
            textSize = mutableBitmap.width / 35f
            isAntiAlias = true
            setShadowLayer(4f, 2f, 2f, AndroidColor.BLACK)
        }

        val margin = 40f
        val textY = mutableBitmap.height - margin
        val lineSpacing = 10f
        
        canvas.drawText(coord, margin, textY, paint)
        canvas.drawText(timestamp, margin, textY - paint.textSize - lineSpacing, paint)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
        
        onComplete(uri)
    } catch (e: Exception) {
        onComplete(uri)
    }
}

/**
 * Resolves a human-readable address from a coordinate string.
 * Supports GeoJSON Point format: {"type":"Point","coordinates":[lon,lat]}
 * and plain "lat,lon" format.
 * Must be called from a background (IO) thread.
 */
private fun resolveAddressFromCoordinates(
    context: android.content.Context,
    coordString: String
): String? {
    return try {
        var lat: Double? = null
        var lon: Double? = null

        // Try GeoJSON format first: {"type":"Point","coordinates":[lon,lat]}
        if (coordString.trimStart().startsWith("{")) {
            val json = org.json.JSONObject(coordString)
            val coords = json.optJSONArray("coordinates")
            if (coords != null && coords.length() >= 2) {
                lon = coords.getDouble(0)
                lat = coords.getDouble(1)
            }
        } else {
            // Try plain "lat,lon" or "lat, lon"
            val parts = coordString.split(",")
            if (parts.size >= 2) {
                lat = parts[0].trim().toDoubleOrNull()
                lon = parts[1].trim().toDoubleOrNull()
            }
        }

        if (lat == null || lon == null) return null

        val geocoder = Geocoder(context, Locale.getDefault())

        if (!Geocoder.isPresent()) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use the modern GeocodeListener API (Android 13+) but since we're on IO thread,
            // we use a blocking approach via a CountDownLatch
            var result: String? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                    result = addresses.firstOrNull()?.let { addr ->
                        buildString {
                            if (!addr.thoroughfare.isNullOrBlank()) append(addr.thoroughfare)
                            if (!addr.subLocality.isNullOrBlank()) {
                                if (isNotBlank()) append(", ")
                                append(addr.subLocality)
                            }
                            if (!addr.locality.isNullOrBlank()) {
                                if (isNotBlank()) append(", ")
                                append(addr.locality)
                            }
                            if (!addr.adminArea.isNullOrBlank()) {
                                if (isNotBlank()) append(", ")
                                append(addr.adminArea)
                            }
                        }.ifBlank { addr.getAddressLine(0) }
                    }
                    latch.countDown()
                }
                override fun onError(errorMessage: String?) {
                    latch.countDown()
                }
            })
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            result
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { addr ->
                buildString {
                    if (!addr.thoroughfare.isNullOrBlank()) append(addr.thoroughfare)
                    if (!addr.subLocality.isNullOrBlank()) {
                        if (isNotBlank()) append(", ")
                        append(addr.subLocality)
                    }
                    if (!addr.locality.isNullOrBlank()) {
                        if (isNotBlank()) append(", ")
                        append(addr.locality)
                    }
                    if (!addr.adminArea.isNullOrBlank()) {
                        if (isNotBlank()) append(", ")
                        append(addr.adminArea)
                    }
                }.ifBlank { addr.getAddressLine(0) }
            }
        }
    } catch (e: Exception) {
        null
    }
}
