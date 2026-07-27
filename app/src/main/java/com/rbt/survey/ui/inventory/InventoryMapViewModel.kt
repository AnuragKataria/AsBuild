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

class InventoryMapViewModel(
    private val repository: AssetRepository
) : ViewModel() {

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

    private val _assetConfig = MutableStateFlow<AssetConfigResponse?>(null)

    val assetConfig = _assetConfig.asStateFlow()

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


    private fun savePdf(
        context: Context,
        body: ResponseBody,
        fileName: String
    ): Pair<Uri, String>? {

        val values = ContentValues().apply {

            put(
                MediaStore.Downloads.DISPLAY_NAME,
                fileName
            )

            put(
                MediaStore.Downloads.MIME_TYPE,
                "application/pdf"
            )

            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS
            )
        }

        val uri =
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: return null

        context.contentResolver
            .openOutputStream(uri)
            ?.use { output ->

                body.byteStream().use { input ->

                    input.copyTo(output)
                }
            }

        var actualFileName = fileName

        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Downloads.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                actualFileName =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Downloads.DISPLAY_NAME
                        )
                    )
            }
        }

        return Pair(
            uri,
            actualFileName
        )
    }

    private fun showDownloadNotification(
        context: Context,
        fileName: String,
        pdfUri: Uri
    ) {

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "downloads",
                "Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            manager.createNotificationChannel(channel)
        }

        val intent = Intent(
            Intent.ACTION_VIEW
        ).apply {

            setDataAndType(
                pdfUri,
                "application/pdf"
            )

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                context,
                "downloads"
            )
                .setSmallIcon(
                    android.R.drawable.stat_sys_download_done
                )
                .setContentTitle("Download Complete")
                .setContentText(fileName)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        manager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }

}