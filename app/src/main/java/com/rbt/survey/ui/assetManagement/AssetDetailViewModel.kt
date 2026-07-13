package com.rbt.survey.ui.assetManagement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.rbt.survey.data.model.*
import com.rbt.survey.data.repository.AssetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*


class AssetDetailViewModel(
    private val repository: AssetRepository
) : ViewModel() {

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading =
        _isLoading.asStateFlow()

    private val _assetDetail =
        MutableStateFlow<AssetDetailResponse?>(null)

    val assetDetail =
        _assetDetail.asStateFlow()

    private val _assetConfig =
        MutableStateFlow<AssetConfigResponse?>(null)

    val assetConfig =
        _assetConfig.asStateFlow()

    private val _canvasFields =
        MutableStateFlow<List<DynamicField>>(emptyList())

    val canvasFields =
        _canvasFields.asStateFlow()

    private val _formFieldMasters =
        MutableStateFlow<List<FieldMaster>>(emptyList())

    val formFieldMasters =
        _formFieldMasters.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    fun loadData(
        assetTypeId: Int
    ) {

        viewModelScope.launch {

            try {

                _isLoading.value = true

                // Selected Assets Details
                 val assetDetail = repository.getAssetDetail(assetTypeId)
                _assetDetail.value =
                    assetDetail

                // Selected Assets Configs
                val assetConfigResponse =
                    repository.getAssetConfig(assetTypeId)

                if (assetConfigResponse.isSuccessful) {

                    val assetConfig = assetConfigResponse.body()

                    _assetConfig.value = assetConfig

                    assetConfig?.data?.currentVersion?.schema?.fields?.let {
                        _canvasFields.value = it
                    }

                } else if (assetConfigResponse.code() == 404) {

                    // No configuration exists yet
                    _assetConfig.value = null
                    _canvasFields.value = emptyList()

                } else {

                    throw Exception(
                        "Asset config error: ${assetConfigResponse.code()}"
                    )
                }

                // Form Field Master
                 val formFieldMaster = repository.getFormFieldMaster()
                _formFieldMasters.value =
                    formFieldMaster.data

            } catch (e: Exception) {

                e.printStackTrace()

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun duplicateField(index: Int) {

        val currentList = _canvasFields.value.toMutableList()
        val originalField = currentList[index]
        val baseId = originalField.id.replace(Regex("_\\d+$"), "")
        var newId = "${baseId}_1"
        var counter = 1

        val existingIds = currentList.map {
            it.id
        }

        while (existingIds.contains(newId)) {
            counter++
            newId = "${baseId}_$counter"
        }

        val duplicatedField = originalField.copy(
            id = newId,
        )

        currentList.add(
            index + 1,
            duplicatedField
        )

        _canvasFields.value = currentList
    }

    fun removeField(index: Int) {
        val currentList = _canvasFields.value.toMutableList()
        currentList.removeAt(index)
        _canvasFields.value = currentList
    }

    fun moveField(
        fromIndex: Int,
        toIndex: Int
    ) {
        val currentList = _canvasFields.value.toMutableList()
        if (
            fromIndex in currentList.indices &&
            toIndex in currentList.indices
        ) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(
                toIndex,
                item
            )
            _canvasFields.value = currentList
        }
    }

    fun addFieldFromMaster(
        fieldMaster: FieldMaster
    ) {

        val currentList = _canvasFields.value.toMutableList()

        val baseId = fieldMaster.fieldCode

        var newId = baseId
        var counter = 1

        val existingIds = currentList.map {
            it.id
        }

        while (existingIds.contains(newId)) {

            newId = "${baseId}_$counter"

            counter++
        }


        val template = fieldMaster.template


        val newField = DynamicField(

            id = newId,

            type = fieldMaster.fieldType,

            label = fieldMaster.fieldName,

            multi = false,

            options = emptyList(),

            isActive = fieldMaster.isActive,

            readOnly = template
                .get("readOnly")
                ?.asBoolean ?: false,

            required = template
                .get("required")
                ?.asBoolean ?: false,

            dataSource = null,

            searchable = false,

            validation = null,

            placeholder = "",

            resetOnHide = template
                .get("resetOnHide")
                ?.asBoolean ?: false,

            defaultValue = template
                .get("defaultValue")
                ?.let {

                    when {

                        it.isJsonNull -> null

                        it.isJsonPrimitive ->
                            it.asJsonPrimitive.asString

                        else ->
                            it.toString()
                    }
                },

            dependencies = emptyList(),

            legacyFieldId = null,

            masterFieldId = fieldMaster.fieldMasterId,

            dependentOptions = null,

            masterColumnName = null,

            resolveFromMaster = false
        )


        currentList.add(
            newField
        )


        _canvasFields.value = currentList
    }

    fun addCustomField(
        fieldCode: String,
        fieldLabel: String,
        dataType: String,
        required: Boolean,
        searchable: Boolean,
        fieldMasterId: Int
    ) {

        val currentList = _canvasFields.value.toMutableList()

        var newId = fieldCode.uppercase()

        var counter = 1

        val existingIds = currentList.map {
            it.id
        }

        while (existingIds.contains(newId)) {

            newId = "${fieldCode.uppercase()}_$counter"

            counter++
        }

        val field = DynamicField(

            id = newId,

            type = dataType.lowercase(),

            label = fieldLabel,

            multi = false,

            options = emptyList(),

            isActive = true,

            readOnly = false,

            required = required,

            dataSource = null,

            searchable = searchable,

            validation = null,

            placeholder = "",

            resetOnHide = true,

            defaultValue = null,

            dependencies = emptyList(),

            legacyFieldId = null,

            masterFieldId = fieldMasterId,

            dependentOptions = null,

            masterColumnName = null,

            resolveFromMaster = false
        )

        currentList.add(field)

        _canvasFields.value = currentList
    }

    fun updateField(updatedField: DynamicField) {

        _canvasFields.value =
            _canvasFields.value.map {

                if (it.id == updatedField.id) {
                    updatedField
                } else {
                    it
                }
            }
    }

    fun saveConfiguration(
        assetTypeId: Int,
        assetCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                _isSaving.value = true

                val request =
                    SaveConfigRequest(

                        schema =
                            SaveSchema(
                                fields = _canvasFields.value
                            ),

                        configMetadata =
                            SaveConfigMetadata(
                                module = "Assets",
                                source = "asset-management",
                                assetCode = assetCode
                            ),

                        layout =
                            SaveLayout(
                                sections = listOf(
                                    SaveSection(
                                        title = "Asset Fields",
                                        fields =
                                            _canvasFields.value.map {
                                                it.id
                                            }
                                    )
                                )
                            ),

                        isPublished = true,

                        createdBy = "admin"
                    )
                val gson = GsonBuilder()
                    .setPrettyPrinting()
                    .create()

                Log.d(
                    "SAVE_REQUEST",
                    gson.toJson(request)
                )

                val response =

                    if (_assetConfig.value == null) {

                        Log.d(
                            "SAVE_CONFIG",
                            "Creating first configuration"
                        )

                        repository.createConfiguration(
                            assetTypeId,
                            request
                        )

                    } else {

                        Log.d(
                            "SAVE_CONFIG",
                            "Creating new version"
                        )

                        repository.saveConfiguration(
                            assetTypeId,
                            request
                        )
                    }

                if (response.isSuccessful) {

                    onSuccess()
                    Log.d(
                        "SAVE_REQUEST",
                        GsonBuilder().setPrettyPrinting().create().toJson(request)
                    )

                } else {

                    Log.e(
                        "SAVE_ERROR",
                        response.errorBody()?.string() ?: "No error body"
                    )
                    onError(
                        "Save failed : ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                onError(
                    e.message ?: "Unknown error"
                )

            } finally {

                _isSaving.value = false
            }
        }
    }
}