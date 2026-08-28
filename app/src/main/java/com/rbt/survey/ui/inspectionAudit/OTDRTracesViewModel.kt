package com.rbt.survey.ui.inspectionAudit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.repository.AssetRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.flow.first

class OTDRTracesViewModel(
    private val repository: AssetRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _siteNames = MutableStateFlow<List<String>>(emptyList())
    val siteNames = _siteNames.asStateFlow()

    private val _otdrReports = MutableStateFlow<List<JsonObject>>(emptyList())
    val otdrReports = _otdrReports.asStateFlow()

    private val _uploadSuccess = MutableSharedFlow<Boolean>()
    val uploadSuccess = _uploadSuccess.asSharedFlow()

    private val _isLoadingmessage = MutableStateFlow("")
    val isLoadingmessage: StateFlow<String> = _isLoadingmessage
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadingSites = MutableStateFlow(false)
    val isLoadingSites = _isLoadingSites.asStateFlow()


    fun loadOTDRSiteNames() {

        viewModelScope.launch {

            _isLoading.value = true
            _isLoadingmessage.value = "Loading Site Data"

            try {

                val result =
                    repository.getOTDRSiteNames()

                if (result.isSuccess) {

                    _siteNames.value =
                        result.getOrDefault(emptyList())

                } else {

                    _siteNames.value =
                        emptyList()
                }

            } catch (e: Exception) {

                _siteNames.value =
                    emptyList()

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun loadOTDRReports() {

        viewModelScope.launch {

            _isLoading.value = true
            _isLoadingmessage.value = "Loading OTDR Uploaded Data"

            try {

                val loggedInUser = preferences.userName.first()

                if (loggedInUser.isNullOrBlank()) {
                    _otdrReports.value = emptyList()
                    return@launch
                }

                val result = repository.getOTDRReports()

                if (result.isSuccess) {

                    val response = result.getOrNull()

                    val data = response?.getAsJsonArray("data")

                    if (data != null) {

                        val filteredReports =
                            data.mapNotNull { element ->

                                if (!element.isJsonObject) {
                                    return@mapNotNull null
                                }

                                val report = element.asJsonObject

                                val uploadedBy = report
                                        .get("reportUploadedBy")
                                        ?.asString

                                if (
                                    uploadedBy.equals(
                                        loggedInUser,
                                        ignoreCase = true
                                    )
                                ) {
                                    report
                                } else {
                                    null
                                }
                            }

                        _otdrReports.value =
                            filteredReports
                    }

                } else {

                    _otdrReports.value =
                        emptyList()
                }

            } catch (e: Exception) {

                _otdrReports.value =
                    emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadTrace(
        file: File,
        fromStation: String,
        toStation: String,
        deviceType: String
    ) {

        viewModelScope.launch {

            _isLoading.value = true
            _isLoadingmessage.value = "Uploading OTDR Trace"
            try {

                val userName = preferences.userName.first()

                val result = repository.uploadOTDRTrace(
                    file = file,
                    fromStation = fromStation,
                    toStation = toStation,
                    uploadedBy = userName.orEmpty(),
                    deviceType = deviceType
                )

                _uploadSuccess.emit(
                    result.isSuccess &&
                            result.getOrNull() == true
                )

            } catch (e: Exception) {

                _uploadSuccess.emit(false)

            } finally {
                _isLoading.value = false
            }
        }
    }

}