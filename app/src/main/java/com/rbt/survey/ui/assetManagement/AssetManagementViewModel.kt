package com.rbt.survey.ui.assetManagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.model.AssetTypeResponse
import com.rbt.survey.data.model.CreateAssetRequest
import com.rbt.survey.data.repository.AssetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody


class AssetManagementViewModel (
    private val repository: AssetRepository
) : ViewModel() {

    private val _assetTypes =
        MutableStateFlow<List<AssetTypeResponse>>(emptyList())

    val assetTypes = _assetTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    private val _createSuccess = MutableStateFlow(false)
    val createSuccess = _createSuccess.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        loadAssets()
    }

    private fun loadAssets() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                _assetTypes.value = repository.getAssetTypes()
            } catch (e: Exception) {
                e.printStackTrace()
            }finally {
                _isLoading.value = false
            }
        }
    }

    fun createAsset(
        request: CreateAssetRequest
    ) {

        viewModelScope.launch {

            try {

                _isCreating.value = true

                val response = repository.createAsset(request)

                if (response.isSuccessful) {

                    _createSuccess.value = true
                    _message.value = "Asset created successfully"

                    loadAssets() // refresh list

                } else {

                    _message.value =
                        "Failed to create asset (${response.code()})"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _message.value = e.message ?: "Something went wrong"
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun createAssetWithImage(
        request: CreateAssetRequest,
        imagePart: MultipartBody.Part
    ) {

        viewModelScope.launch {

            try {

                _isCreating.value = true

                val response = repository.createAssetWithImage(

                        assetCode = request.assetCode
                            .toRequestBody("text/plain".toMediaType()),

                        assetName = request.assetName
                            .toRequestBody("text/plain".toMediaType()),

                        assetCategory = request.assetCategory
                            .toRequestBody("text/plain".toMediaType()),

                        assetnature = request.assetnature
                            .toRequestBody("text/plain".toMediaType()),

                        geometryType = request.geometryType
                            .toRequestBody("text/plain".toMediaType()),

                        image = imagePart
                    )

                if (response.isSuccessful) {

                    _createSuccess.value = true
                    _message.value = "Asset created successfully"

                    loadAssets() // refresh list

                } else {

                    _message.value =
                        "Failed to create asset (${response.code()})"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _message.value = e.message ?: "Something went wrong"
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun resetCreateSuccess() {
        _createSuccess.value = false
    }

    fun clearMessage() {
        _message.value = null
    }
}