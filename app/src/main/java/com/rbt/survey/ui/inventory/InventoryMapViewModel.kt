package com.rbt.survey.ui.inventory

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.model.*
import com.rbt.survey.data.repository.AssetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.maps.android.compose.MapType
import com.rbt.survey.data.utils.savePdf
import com.rbt.survey.data.utils.showDownloadNotification
import okhttp3.RequestBody
import org.json.JSONObject

class InventoryMapViewModel(
    private val repository: AssetRepository
) : ViewModel() {

    private val _mapType = MutableStateFlow(MapType.NORMAL)
    val mapType: StateFlow<MapType> = _mapType

    private val _projects = MutableStateFlow<List<ProjectResponse>>(emptyList())
    val projects: StateFlow<List<ProjectResponse>> = _projects

    private val _connectivityRules = MutableStateFlow<List<ConnectivityRuleResponse>>(emptyList())
    val connectivityRules = _connectivityRules.asStateFlow()

    private val _createdAssets = MutableStateFlow<List<CreatedAssetData>>(emptyList())
    val createdAssets = _createdAssets.asStateFlow()

    private val _allassetDetails = MutableStateFlow<List<AssetDetailResponse>>(emptyList())
    val allassetDetails: StateFlow<List<AssetDetailResponse>> = _allassetDetails

    private val _assetDetails = MutableStateFlow<AssetDetailResponse?>(null)
    val assetDetails: StateFlow<AssetDetailResponse?> = _assetDetails

    private val _createdAssetDetails = MutableStateFlow<CreatedAssetDetailsData?>(null)
    val createdAssetDetails: StateFlow<CreatedAssetDetailsData?> = _createdAssetDetails

    private val _assetConfig = MutableStateFlow<AssetConfigResponse?>(null)
    val assetConfig = _assetConfig.asStateFlow()

    private val _customers = MutableStateFlow<List<CustomerResponse>>(emptyList())
    val customers: StateFlow<List<CustomerResponse>> = _customers

    private val _fmsutilization = MutableStateFlow<List<FmsPortUtilizationResponse>>(emptyList())
    val fmsutilization = _fmsutilization.asStateFlow()

    private val _terminationfibercorestructure = MutableStateFlow<FiberStructureResponse?>(null)
    val terminationfibercorestructure: StateFlow<FiberStructureResponse?> = _terminationfibercorestructure

    private val _leftfibercorestructure = MutableStateFlow<FiberStructureResponse?>(null)
    val leftfibercorestructure: StateFlow<FiberStructureResponse?> = _leftfibercorestructure

    private val _rightfibercorestructure = MutableStateFlow<FiberStructureResponse?>(null)
    val rightfibercorestructure: StateFlow<FiberStructureResponse?> = _rightfibercorestructure

    private val _terminationFiberCoreUtilization = MutableStateFlow<List<FiberCoreUtilizationResponse>>(emptyList())
    val terminationfibercoreutilization: StateFlow<List<FiberCoreUtilizationResponse>> = _terminationFiberCoreUtilization

    private val _leftFiberCoreUtilization = MutableStateFlow<List<FiberCoreUtilizationResponse>>(emptyList())
    val leftfibercoreutilization: StateFlow<List<FiberCoreUtilizationResponse>> = _leftFiberCoreUtilization

    private val _rightFiberCoreUtilization = MutableStateFlow<List<FiberCoreUtilizationResponse>>(emptyList())
    val rightfibercoreutilization: StateFlow<List<FiberCoreUtilizationResponse>> = _rightFiberCoreUtilization

    private val _terminationResult = MutableStateFlow<String?>(null)
    val terminationResult = _terminationResult.asStateFlow()

    private val _spliceResult = MutableStateFlow<String?>(null)
    val spliceResult = _spliceResult.asStateFlow()

    private val _customerMappings = MutableStateFlow<List<CustomerMappingResponse>>(emptyList())
    val customerMappings = _customerMappings.asStateFlow()

    private val _customerMappingResponse = MutableStateFlow<JSONObject?>(null)

    val customerMappingResponse = _customerMappingResponse.asStateFlow()

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

                _allassetDetails.value = details

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAssetLoading.value = false
            }
        }
    }

    fun loadAssetDetails(assetTypeId: Int) {

        viewModelScope.launch {
            try {
                _isAssetLoading.value = true
                _isLoadingmessage.value = "Loading Assets..."

                val details = repository.getAssetDetail(assetTypeId)

                _assetDetails.value = details

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAssetLoading.value = false
            }
        }
    }

    fun loadCreatedAssetDetails(assetId: Int) {

        viewModelScope.launch {
            try {
                _isAssetLoading.value = true
                _isLoadingmessage.value = "Loading Assets Deatils..."

                val details = repository.getCretedAssetDetail(assetId)

                _createdAssetDetails.value = details.data

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

                } else if (assetConfigResponse.code() == 404) {

                    // No configuration exists yet
                    _assetConfig.value = null

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

    fun setMapType(type: MapType) {
        _mapType.value = type
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

                _createdAssets.value = response.data

            } catch (e: Exception) {

                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun exportPdf(
        assetId: Int,
        context: Context
    ) {

        viewModelScope.launch {

            try {
                _isLoading.value = true
                val response =
                    repository.exportPdf(assetId)

                if (response.isSuccessful) {

                    response.body()?.let {

                        val savedPdf = savePdf(
                            context,
                            it,
                            "Asset_$assetId.pdf"
                        )

                        savedPdf?.let { (uri, fileName) ->

                            showDownloadNotification(
                                context,
                                fileName,
                                uri
                            )
                        }

                        _saveMessage.value =
                            "PDF downloaded successfully"
                    }

                } else {

                    _saveMessage.value =
                        "No data available to download"
                }

            } catch (e: Exception) {

                _saveMessage.value =
                    e.message ?: "Failed to download PDF"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadcustomers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Fetching Customers"
                _customers.value = repository.getcustomers()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFMSUtilization(assetId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Fetching FMS Utilization"
                _fmsutilization.value = repository.getFmsUtilization(assetId)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFiberCoreStructure(assetId: Int, side: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Fetching Fibercore Structure"
                val result = repository.getFiberCoreStructure(assetId)
                when (side) {
                    "TerminationFiber" -> {
                        _terminationfibercorestructure.value = result
                    }

                    "LeftFiber" -> {
                        _leftfibercorestructure.value = result
                    }

                    "RightFiber" -> {
                        _rightfibercorestructure.value = result
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFiberCoreUtilization(assetId: Int,side: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Fetching Fibercore Utilization"
                val result = repository.getFiberCoreUtilization(assetId)
                when (side) {
                    "TerminationFiber" -> {
                        _terminationFiberCoreUtilization.value = result
                    }

                    "LeftFiber" -> {
                        _leftFiberCoreUtilization.value = result
                    }

                    "RightFiber" -> {
                        _rightFiberCoreUtilization.value = result
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearFMSandFiberUtilization() {
        _fmsutilization.value = emptyList()
        _terminationfibercorestructure.value = null
        _terminationFiberCoreUtilization.value = emptyList()
    }

    fun clearFiberStructureandFiberUtilization() {
        _leftfibercorestructure.value = null
        _leftFiberCoreUtilization.value = emptyList()
        _rightfibercorestructure.value = null
        _rightFiberCoreUtilization.value = emptyList()
    }

    fun createTermination(request: JSONObject) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Creating Termination"
                val result =
                    repository.createTermination(request)
                if (result.isSuccessful) {
                    _terminationResult.value =
                        "SUCCESS"
                    Log.d(
                        "TERMINATION",
                        "Created Successfully"
                    )

                } else {
                    _terminationResult.value =
                        result.errorBody()?.string()
                            ?: "Unknown Error"
                    Log.e(
                        "TERMINATION",
                        result.errorBody()?.string() ?: "Unknown Error"
                    )
                }


            } catch (e: Exception) {
                _terminationResult.value =
                    e.message ?: "Something went wrong"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createSplice(request: JSONObject) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Creating Splice"
                val result =
                    repository.createSplice(request)
                if (result.isSuccessful) {
                    _spliceResult.value =
                        "SUCCESS"
                    Log.d(
                        "splice",
                        "Created Successfully"
                    )

                } else {
                    _spliceResult.value =
                        result.errorBody()?.string()
                            ?: "Unknown Error"
                    Log.e(
                        "splice",
                        result.errorBody()?.string() ?: "Unknown Error"
                    )
                }


            } catch (e: Exception) {
                _spliceResult.value =
                    e.message ?: "Something went wrong"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePortHealthStatus(request: JSONObject,assetId: Int,onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Updating Port Status"
                repository.updatePortHealthStatus(request)
                loadCreatedAssetDetails(assetId)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCustomerMappings(assetId: Int) {

        viewModelScope.launch {

            try {

                _isLoading.value = true
                _isLoadingmessage.value = "Fetchng Customer Mapping"

                _customerMappings.value =
                    repository.getCustomerMappings(assetId)

            } catch (e: Exception) {

                e.printStackTrace()

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun updateCustomerPort(request: JSONObject,onSuccess: () -> Unit,onMapConflict: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _isLoadingmessage.value = "Updating Customer"

                val response = repository.updateCustomerPort(request)

                val responseText =
                    if (response.isSuccessful) {
                        response.body()?.string()
                    } else {
                        response.errorBody()?.string()
                    }

                val json = JSONObject(responseText ?: "{}")

                _customerMappingResponse.value = json

                if (json.optString("resultType") == "MAPPED") {
                    onSuccess()
                }else if(json.optString("resultType") == "CUSTOMER_CONFLICT"){
                    onMapConflict()
                }else if(json.optString("resultType") == "ALREADY_MAPPED"){
                    onSuccess()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportSpliceClosureDiagramPdf(
        assetId: Int,
        context: Context
    ) {

        viewModelScope.launch {

            try {
                _isLoading.value = true
                val response =
                    repository.exportSpliceClosureDiagramPdf(assetId)

                if (response.isSuccessful) {

                    response.body()?.let {

                        val savedPdf = savePdf(
                            context,
                            it,
                            "SpliceClosure_$assetId.pdf"
                        )

                        savedPdf?.let { (uri, fileName) ->

                            showDownloadNotification(
                                context,
                                fileName,
                                uri
                            )
                        }

                        _saveMessage.value =
                            "PDF downloaded successfully"
                    }

                } else {

                    _saveMessage.value =
                        "No data available to download"
                }

            } catch (e: Exception) {

                _saveMessage.value =
                    e.message ?: "Failed to download PDF"
            } finally {
                _isLoading.value = false
            }
        }
    }

}