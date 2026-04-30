package com.rbt.survey.ui.form

import androidx.compose.runtime.mutableStateMapOf
import com.rbt.survey.data.local.db.OfflineSubmission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.model.Condition
import com.rbt.survey.data.model.FormDetailData
import com.rbt.survey.data.model.FormField
import com.rbt.survey.data.model.FormOption
import com.rbt.survey.data.repository.FormRepository
import com.rbt.survey.dgps.DgpsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

sealed class FormUiState {
    object Loading : FormUiState()
    data class Success(val formData: FormDetailData) : FormUiState()
    data class Error(val message: String) : FormUiState()
}

class FormDataCollectionViewModel(
    private val formId: Int,
    private val blockCode: String?,
    private val repository: FormRepository,
    private val preferences: UserPreferences,
    private val selectedGpName: String?,
    private val dgpsManager: DgpsManager,
    private val submissionId: Int? = null,
    private val radius: Int? = null
) : ViewModel() {
    private val saveJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    val surveyRadius: Int? = radius
    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess

    private val _uiState = MutableStateFlow<FormUiState>(FormUiState.Loading)
    val uiState: StateFlow<FormUiState> = _uiState

    val dgpsLocation = dgpsManager.location
    val dgpsStatus = dgpsManager.status
    val useDgps = preferences.useDgps

    // Map to store current values of each field: fieldId -> value
    val fieldValues = mutableStateMapOf<String, Any?>()

    init {
        fetchFormDetail()
        loadDrafts()
    }

    private val gson = com.google.gson.Gson()

    private fun loadDrafts() {
        if (submissionId != null && submissionId != -1) {
            viewModelScope.launch {
                val submission = repository.getOfflineSubmissionById(submissionId)
                if (submission != null) {
                    try {
                        val jsonObject = com.google.gson.JsonParser.parseString(submission.submissionData).asJsonObject
                        if (jsonObject.has("data")) {
                            val dataObject = jsonObject.getAsJsonObject("data")
                            dataObject.entrySet().forEach { entry ->
                                val value = if (entry.value.isJsonPrimitive) {
                                    val primitive = entry.value.asJsonPrimitive
                                    if (primitive.isNumber) {
                                        if (primitive.asString.contains(".")) primitive.asDouble else primitive.asLong
                                    } else if (primitive.isBoolean) {
                                        primitive.asBoolean
                                    } else {
                                        primitive.asString
                                    }
                                } else if (entry.value.isJsonArray) {
                                    gson.fromJson(entry.value, List::class.java)
                                } else {
                                    entry.value.toString()
                                }
                                fieldValues[entry.key] = value
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            viewModelScope.launch {
                val drafts = repository.getDrafts(formId, selectedGpName ?: "")
                drafts.forEach { draft ->
                    val value: Any? = try {
                        when {
                            draft.value.startsWith("[") -> {
                                gson.fromJson(draft.value, List::class.java)
                            }
                            draft.value.toDoubleOrNull() != null -> {
                                if (draft.value.contains(".")) draft.value.toDouble() else draft.value.toLong()
                            }
                            else -> draft.value
                        }
                    } catch (e: Exception) {
                        draft.value
                    }
                    fieldValues[draft.fieldId] = value
                }
            }
        }
    }

    private fun fetchFormDetail() {
        viewModelScope.launch {
            _uiState.value = FormUiState.Loading
            try {
                val response = repository.getFormDetail(formId, blockCode)

                if (response.isSuccessful && response.body() != null) {

                    val gson = Gson()

                    // ✅ Convert to JSON string
                    val jsonString = gson.toJson(response.body())

                    // ✅ Pretty print JSON (formatted)
                    val prettyJson = com.google.gson.JsonParser
                        .parseString(jsonString)
                        .asJsonObject

                    val formattedJson = gson.toJson(prettyJson)

                    android.util.Log.d("FORM_API_JSON", formattedJson)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data!!

                    _uiState.value = FormUiState.Success(data)
                    
//                    // Initialize default values if not already present from drafts
//                    data.currentVersion?.schema?.fields?.forEach { field ->
//                        if (!fieldValues.containsKey(field.id) && field.defaultValue != null) {
//                            fieldValues[field.id] = field.defaultValue
//                        }
//                    }

                    data.currentVersion?.schema?.fields?.forEach { field ->

                        // ✅ Initialize ALL fields properly
                        if (!fieldValues.containsKey(field.id)|| fieldValues[field.id] == null) {

                            when {
                                // 🔥 Hidden fields (force include)
                                field.type == "hidden" -> {
                                    fieldValues[field.id] = field.defaultValue ?: ""
                                }

                                // Default fields
                                field.defaultValue != null -> {
                                    fieldValues[field.id] = field.defaultValue
                                }
                            }
                        }
                    }

                    val fields = data.currentVersion?.schema?.fields ?: emptyList()

                    if (!selectedGpName.isNullOrEmpty()) {
                        fieldValues["GP"] = selectedGpName

                        // 🔥 IMPORTANT: Only auto-set location if NOT already saved
                        val mapField = fields.find { it.type == "map" }

                        if (mapField != null) {
                            val existingValue = fieldValues[mapField.id]

                            if (existingValue == null || existingValue.toString().isBlank()) {
                                updateDependentMapFields("GP", selectedGpName, fields)
                            }
                        }
                    }
                } else {
                    _uiState.value = FormUiState.Error(response.body()?.message ?: "Failed to load form details")
                }
            } catch (e: Exception) {
                _uiState.value = FormUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun onFieldValueChange(fieldId: String, newValue: Any?, fields: List<FormField>, context: android.content.Context? = null) {
        // If it's a file field and we have a context, check size before updating state
        val field = fields.find { it.id == fieldId }
        val isFileType = field != null && (field.type == "image" || field.type == "video" || field.type == "file" || field.type == "document")
        
        if (context != null && isFileType && newValue != null) {
            val uriString = newValue.toString()
            if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                val size = getFileSize(context, android.net.Uri.parse(uriString))
                val maxSize = 20 * 1024 * 1024 // 20 MB
                if (size > maxSize) {
                    android.widget.Toast.makeText(context, "File size is too large (Max 20MB)", android.widget.Toast.LENGTH_LONG).show()
                    return // Stop further processing
                }
            }
        }

        fieldValues[fieldId] = newValue
        // 🔥 Auto update dependent map fields (like GPLocation)
        updateDependentMapFields(fieldId, newValue, fields)

        // 🔥 Handle conditional visibility + resetOnHide
        fields.forEach { f ->
            val shouldShow = shouldShowField(f)

            if (!shouldShow && f.resetOnHide == true) {
                if (fieldValues.containsKey(f.id)) {
                    fieldValues.remove(f.id)

                    saveJobs[f.id]?.cancel()
                    saveJobs[f.id] = viewModelScope.launch {
                        repository.saveDraft(formId, f.id,selectedGpName ?: "", "")
                    }
                }
            }
        }
        
        // Save to Local DB immediately after entry to avoid data loss
        saveJobs[fieldId]?.cancel()
        saveJobs[fieldId] = viewModelScope.launch {
            val stringValue = if (newValue is List<*> || newValue is Map<*, *>) {
                gson.toJson(newValue)
            } else {
                newValue?.toString() ?: ""
            }
            repository.saveDraft(formId, fieldId,selectedGpName ?: "", stringValue)
        }

        // Trigger immediate upload if it's a file type
        if (context != null && newValue != null && isFileType) {
            val uriString = newValue.toString()
            if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                uploadIndividualFile(context, fieldId, uriString)
            }
        }
        
        // Handle dependencies...
        fields.forEach { f ->
            f.dependencies?.forEach { dep ->
                if (dep.field == fieldId && dep.clearOnParentChange) {
                    fieldValues.remove(f.id)
                    saveJobs[f.id]?.cancel()
                    saveJobs[f.id] = viewModelScope.launch { repository.saveDraft(formId, f.id,selectedGpName ?: "", "") }
                    onFieldValueChange(f.id, null, fields, context) 
                }
            }
        }
    }

    private fun updateDependentMapFields(
        changedFieldId: String,
        newValue: Any?,
        fields: List<FormField>
    ) {
        fields.forEach { f ->

            if (f.type == "map" && f.dependentOptions != null) {

                val dep = f.dependentOptions as? Map<*, *> ?: return@forEach

                val sourceField = dep["sourceField"]?.toString() ?: return@forEach
                val sourceProperty = dep["sourceProperty"]?.toString() ?: return@forEach

                if (sourceField == changedFieldId) {

                    val parentValue = newValue?.toString() ?: return@forEach
                    val parentField = fields.find { it.id == sourceField } ?: return@forEach

                    val selectedOption = parentField.options?.firstOrNull { option ->
                        try {
                            val json = gson.toJsonTree(option).asJsonObject
                            json.get("Value")?.asString == parentValue
                        } catch (e: Exception) {
                            false
                        }
                    }

                    if (selectedOption != null) {
                        try {
                            val json = gson.toJsonTree(selectedOption).asJsonObject
                            val raw = json.getAsJsonObject("Raw")

                            val locationValue =
                                raw?.get("NewGpLocation")
                                    ?: raw?.get(sourceProperty)

                            if (locationValue != null && !locationValue.isJsonNull) {

                                val valueStr = if (locationValue.isJsonPrimitive) {
                                    locationValue.asString
                                } else {
                                    locationValue.toString()
                                }

// 🔥 Convert immediately to GeoJSON
                                val geoJson = convertLatLngToGeoJson(valueStr)

// ✅ Store proper format
                                fieldValues[f.id] = geoJson ?: valueStr

                                android.util.Log.d("MAP_INIT", "Auto-set ${f.id} = $valueStr")

                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MAP_INIT", "Error parsing", e)
                        }
                    }
                }
            }
        }
    }

    private fun convertLatLngToGeoJson(value: String): Map<String, Any>? {
        return try {
            val json = gson.fromJson(value, Map::class.java)

            val lat = (json["lat"] as? Double)
            val lng = (json["lng"] as? Double)

            if (lat != null && lng != null) {
                mapOf(
                    "type" to "Point",
                    "coordinates" to listOf(lng, lat) // ⚠️ [lng, lat]
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun shouldShowField(field: FormField): Boolean {
        val logic = field.conditionalLogic ?: return true

        val results = logic.conditions.map { condition ->

            val parentValue = fieldValues[condition.field]

            when (parentValue) {
                is List<*> -> {
                    parentValue.any {
                        evaluateCondition(it?.toString(), condition)
                    }
                }
                else -> {
                    evaluateCondition(parentValue?.toString(), condition)
                }
            }
        }

        return if (logic.logic.uppercase() == "AND") {
            results.all { it }
        } else {
            results.any { it }
        }
    }

    private fun evaluateCondition(value: String?, condition: Condition): Boolean {
        if (value == null) return false

        return when (condition.operator.lowercase()) {
            "contains" -> value.contains(condition.value, ignoreCase = true)
            "equals" -> value.equals(condition.value, ignoreCase = true)
            "not_equals" -> !value.equals(condition.value, ignoreCase = true)
            "in" -> condition.value.split(",").any { it.trim().equals(value, true) }
            else -> false
        }
    }

    private fun getFileSize(context: android.content.Context, uri: android.net.Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) cursor.getLong(sizeIndex) else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun uploadIndividualFile(context: android.content.Context, fieldId: String, uriString: String) {
        viewModelScope.launch {
            try {
                val userName = preferences.userName.first() ?: "User"
                val response = repository.uploadFile(
                    context = context,
                    formId = formId,
                    fieldId = fieldId,
                    uploadedBy = userName,
                    uriString = uriString
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    val fileData = response.body()?.data
                    // The user wants the response saved in the local database
                    // We can save the fileId or the whole JSON response as the field value
                    if (fileData != null) {
                        val responseJson = gson.toJson(fileData)
                        repository.saveDraft(formId, fieldId,selectedGpName ?: "", responseJson)
                        // Also update memory state so the UI reflects the uploaded status
                        fieldValues[fieldId] = responseJson
                    }
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun getOptionsForField(field: FormField): List<FormOption> {
        // 1. Check if options are in the field itself
        if (field.options != null) {
            return field.options.map { option ->
                when (option) {
                    is String -> FormOption(option, option)
                    is Map<*, *> -> {
                        val label = (option["Label"] ?: option["label"])?.toString() ?: ""
                        val value = (option["Value"] ?: option["value"])?.toString() ?: ""
                        FormOption(label, value)
                    }
                    else -> {
                        // Handle possible GSON internal representation if it's a JsonObject
                        try {
                            val jsonString = gson.toJson(option)
                            gson.fromJson(jsonString, FormOption::class.java)
                        } catch (e: Exception) {
                            FormOption(option.toString(), option.toString())
                        }
                    }
                }
            }
        }

        // 2. Check dependent options
        if (field.dependentOptions != null && field.dependencies != null) {

            // ❗ skip if API sent STRING instead of object
            if (field.dependentOptions is String) {
                return emptyList()
            }

            val parentDep = field.dependencies.firstOrNull()
            if (parentDep != null) {

                val parentValue = fieldValues[parentDep.field]
                if (parentValue != null) {

                    val depMap = field.dependentOptions as? Map<*, *> ?: return emptyList()

                    val optionsForParentField =
                        depMap[parentDep.field] as? Map<*, *> ?: return emptyList()

                    val result =
                        optionsForParentField[parentValue.toString()] as? List<*>
                            ?: return emptyList()

                    return result.mapNotNull { item ->
                        when (item) {
                            is FormOption -> item
                            is Map<*, *> -> {
                                val label = item["Label"]?.toString() ?: ""
                                val value = item["Value"]?.toString() ?: ""
                                FormOption(label, value)
                            }
                            else -> null
                        }
                    }
                }
            }
        }

//        // 3. Check geoContext levels
//        val successState = uiState.value as? FormUiState.Success
//        val level = successState?.formData?.geoContext?.levels?.find { it.levelCode == field.id }
//        if (level?.options != null) {
//            return level.options
//        }

        return emptyList()
    }

    fun submitForm(context: android.content.Context, fields: List<FormField>) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is FormUiState.Success) return@launch
            
            val formData = currentState.formData
            val userName = preferences.userName.first() ?: "RBT"
            
            val dataMap = mutableMapOf<String, Any?>()
            val dropdownSnapshot = mutableMapOf<String, Any>()
            val uploadedFileIds = mutableListOf<Int>()
            var terminalBlockCode: String? = blockCode

            // 1. First handle ALL fields from schema (IMPORTANT)
            fields.forEach { field ->

                val value = fieldValues[field.id]

                // -----------------------------
                // ✅ HANDLE DROPDOWN SNAPSHOT
                // -----------------------------
                if (field.type == "dropdown" && value != null) {

                    val selectedOption = field.options?.map {
                        when (it) {
                            is String -> FormOption(it, it)
                            is Map<*, *> -> FormOption(
                                it["label"]?.toString() ?: it["Label"]?.toString() ?: "",
                                it["value"]?.toString() ?: it["Value"]?.toString() ?: ""
                            )
                            else -> null
                        }
                    }?.filterNotNull()
                        ?.find { it.value == value.toString() }

                    val label = selectedOption?.label ?: value.toString()

                    // 👉 KEY = field.label (like "State")
                    // ✅ CORRECT
                    dropdownSnapshot[field.id] = label

                    // also store in data
                    dataMap[field.id] = label
                }

                // -----------------------------
                // ✅ HANDLE HIDDEN FIELDS
                // -----------------------------
                else if (field.type == "hidden") {

                    val dep = field.dependentOptions as? Map<*, *> ?: return@forEach

                    val sourceField = dep["sourceField"]?.toString() ?: return@forEach
                    val sourceProperty = dep["sourceProperty"]?.toString() ?: return@forEach

                    val parentValue = fieldValues[sourceField] ?: return@forEach

                    val parentField = fields.find { it.id == sourceField } ?: return@forEach

                    val selectedOption = parentField.options?.mapNotNull { option ->
                        when (option) {
                            is Map<*, *> -> option
                            else -> null
                        }
                    }?.find { opt ->
                        opt["Value"]?.toString() == parentValue.toString()
                    }

                    val raw = selectedOption?.get("Raw") as? Map<*, *>

                    val hiddenValue = raw?.get(sourceProperty)

                    if (hiddenValue != null) {
                        dataMap[field.id] = hiddenValue.toString()
                    }
                }

                // -----------------------------
                // ✅ HANDLE NORMAL FIELDS
                // -----------------------------
                else if (value != null) {

                    when {
                        // File
                        value is String && value.startsWith("{") && value.contains("fileId") -> {
                            try {
                                val fileData = gson.fromJson(
                                    value,
                                    com.rbt.survey.data.model.FileUploadData::class.java
                                )
                                uploadedFileIds.add(fileData.fileId)
                                dataMap[field.id] = fileData.fileId
                            } catch (e: Exception) {
                                dataMap[field.id] = value
                            }
                        }

                        // Map
                        field.type == "map" -> {
                            when (value) {

                                // ✅ Already proper GeoJSON (after your conversion)
                                is Map<*, *> -> {
                                    dataMap[field.id] = value
                                }

                                // ✅ Old string format (lat,lng OR raw string)
                                is String -> {
                                    val mapValue = formatGeoJson(field, value)
                                    dataMap[field.id] = mapValue ?: value
                                }

                                else -> {
                                    dataMap[field.id] = value
                                }
                            }
                        }

                        // Default
                        else -> {
                            dataMap[field.id] = value
                        }
                    }
                }
            }

            // Construct the final Submission Request object
            val submissionRequest = mutableMapOf<String, Any?>(
                "parentSubmissionId" to null,
                "updatedBy" to userName,
                "uploadedFileIds" to uploadedFileIds,
                "dropdownSnapshot" to dropdownSnapshot,
                "data" to dataMap,
                "BlockCode" to (terminalBlockCode ?: blockCode)
            )

            // 🔥 Remove nulls (important)
            val cleanedRequest = submissionRequest.filterValues { it != null }

            // 🔥 Convert to JSON
            val gson = Gson()
            val jsonRequest = gson.toJson(cleanedRequest)

            // 🔥 Convert to RequestBody
            val requestBody = jsonRequest.toRequestBody("application/json".toMediaType())

//            val jsonRequest = gson.toJson(submissionRequest)
            
            // Print to Logcat
            android.util.Log.d("SurveySubmission", "====================================================")
            android.util.Log.d("SurveySubmission", "CONSTRUCTED SUBMISSION REQUEST:")
            android.util.Log.d("SurveySubmission", jsonRequest)
            android.util.Log.d("SurveySubmission", "====================================================")
            
            _uiState.value = FormUiState.Loading
            try {
                // Save offline instead of calling the API
                val submission = OfflineSubmission(
                    id = if (submissionId != null && submissionId != -1) submissionId else 0,
                    formId = formId,
                    blockCode = terminalBlockCode ?: blockCode,
                    gp = dataMap["GP"]?.toString(),
                    submissionData = jsonRequest,
                    surveyRadius = surveyRadius
                )
                repository.saveOfflineSubmission(submission)
                
                android.widget.Toast.makeText(context, "Form saved offline successfully!", android.widget.Toast.LENGTH_LONG).show()
                // Clear drafts on success
                repository.clearDrafts(formId, selectedGpName ?: "")
                fieldValues.clear()

                _submitSuccess.value = true
                _uiState.value = currentState
            } catch (e: Exception) {
                android.util.Log.e("SurveySubmission", "Exception", e)
                android.widget.Toast.makeText(context, "Exception: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                _uiState.value = FormUiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    private fun formatGeoJson(field: FormField, rawValue: String): Map<String, Any>? {
        if (rawValue.isBlank()) return null
        
        return try {
            val type = when {
                field.geometryType?.any { it.uppercase().contains("POLYGON") } == true -> "Polygon"
                field.geometryType?.any { it.uppercase().contains("LINE") } == true -> "Polyline"
                else -> "Point"
            }

            when (type) {
                "Point" -> {
                    val coords = rawValue.split(",")
                    if (coords.size == 2) {
                        mapOf(
                            "type" to "Point",
                            "coordinates" to listOf(coords[1].toDouble(), coords[0].toDouble()) // [lon, lat]
                        )
                    } else null
                }
                "Polygon" -> {
                    val listType = object : com.google.gson.reflect.TypeToken<List<LatLng>>() {}.type
                    val points = gson.fromJson<List<LatLng>>(rawValue, listType)
                    if (points.isNullOrEmpty()) return null
                    
                    // Polygon coordinates are a nested list: [[[lon, lat], [lon, lat], ...]]
                    // And typically the last point must match the first.
                    val coords = points.map { listOf(it.longitude, it.latitude) }.toMutableList()
                    if (coords.firstOrNull() != coords.lastOrNull()) {
                        coords.add(coords.first())
                    }
                    
                    mapOf(
                        "type" to "Polygon",
                        "coordinates" to listOf(coords)
                    )
                }
                "Polyline" -> {
                    val listType = object : com.google.gson.reflect.TypeToken<List<com.google.android.gms.maps.model.LatLng>>() {}.type
                    val points = gson.fromJson<List<com.google.android.gms.maps.model.LatLng>>(rawValue, listType)
                    if (points.isNullOrEmpty()) return null
                    
                    mapOf(
                        "type" to "Polyline",
                        "coordinates" to points.map { listOf(it.longitude, it.latitude) }
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
