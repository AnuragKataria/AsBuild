package com.rbt.survey.ui.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.model.*
import com.rbt.survey.data.repository.AssetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InventoryMapViewModel(
    private val repository: AssetRepository
) : ViewModel() {

    private val _projects = MutableStateFlow<List<ProjectResponse>>(emptyList())
    val projects: StateFlow<List<ProjectResponse>> = _projects

    private val _connectivityRules = MutableStateFlow<List<ConnectivityRuleResponse>>(emptyList())
    val connectivityRules = _connectivityRules.asStateFlow()

    private val _createdAssets = MutableStateFlow<List<CreatedAssetData>>(emptyList())
    val createdAssets = _createdAssets.asStateFlow()

    private val _assetDetails = MutableStateFlow<List<AssetDetailResponse>>(emptyList())
    val assetDetails: StateFlow<List<AssetDetailResponse>> = _assetDetails

    private val _assetConfig = MutableStateFlow<AssetConfigResponse?>(null)

    val assetConfig = _assetConfig.asStateFlow()

    private val _canvasFields = MutableStateFlow<List<DynamicField>>(emptyList())

    val canvasFields = _canvasFields.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingmessage = MutableStateFlow("")
    val isLoadingmessage: StateFlow<String> = _isLoadingmessage

    private val _isAssetLoading = MutableStateFlow(false)
    val isAssetLoading: StateFlow<Boolean> = _isAssetLoading

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage = _saveMessage.asStateFlow()

    fun loadProjects() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Fetching Projects"
                _projects.value = repository.getProjects()
                Log.d(
                    "ASSET_RESPONSE",
                    "${_projects.value}}"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadConnectivityRules() {
        viewModelScope.launch {
            repository.getConnectivityRules()
                .onSuccess {
                    _connectivityRules.value = it
                }
                .onFailure {
                    Log.e(
                        "InventoryMap",
                        it.message ?: ""
                    )
                }
        }
    }

    fun loadAssets() {

        viewModelScope.launch {
            try {
                _isAssetLoading.value = true
                _isLoadingmessage.value = "Loading Assets..."

                val assets = repository.getAssetTypes()

                val details = assets.mapNotNull { asset ->
                    try {
                        repository.getAssetDetail(asset.assetTypeID)
                    } catch (e: Exception) {
                        null
                    }
                }

                _assetDetails.value = details

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAssetLoading.value = false
            }
        }
    }

    fun loadAssetConfig(
        assetTypeId: Int
    ) {

        viewModelScope.launch {

            try {

                _isLoading.value = true
                _isLoadingmessage.value = "Fetching Data"

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

            } catch (e: Exception) {

                e.printStackTrace()

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun saveAddAsset(
        request: CreateAddAssetRequest,
        onSuccess: (Int) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val assetId =
                    repository.createAddAsset(request)

                onSuccess(assetId)


            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    fun updateDynamicFields(
        assetId: Int,
        request: UpdateDynamicFieldsRequest
    ) {

        viewModelScope.launch {

            try {

                repository.updateDynamicFields(
                    assetId,
                    request
                )

                _saveMessage.value = "Asset saved successfully"

            } catch (e: Exception) {

                e.printStackTrace()
                _saveMessage.value =
                    e.message ?: "Failed to save asset"
            }
        }
    }

    fun resetSaveMessage() {
        _saveMessage.value = null
    }

    fun loadCreatedAssets(
        projectId: Int
    ) {

        viewModelScope.launch {

            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Fetching Assets"

                val response =
                    repository.getCreatedAssets(projectId)
                if (response.success) {
                    _createdAssets.value =
                        response.data
                }

            } catch (e: Exception) {

                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

}