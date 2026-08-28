package com.rbt.survey.ui.incidentManagement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.model.IncidentDetailsResponse
import com.rbt.survey.data.model.IncidentImpactAssetDetails
import com.rbt.survey.data.repository.AssetRepository
import com.rbt.survey.data.utils.savePdf
import com.rbt.survey.data.utils.showDownloadNotification
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class IncidentDetailsViewModel(
    private val repository: AssetRepository
) : ViewModel() {

    private val _incidentDetails = MutableStateFlow<IncidentDetailsResponse?>(null)
    val incidentDetails = _incidentDetails.asStateFlow()

    private val _impactAssets = MutableStateFlow<List<IncidentImpactAssetDetails>>(emptyList())
    val impactAssets = _impactAssets.asStateFlow()

    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment = _isPostingComment.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage = _saveMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun loadIncidentDetails(
        incidentId: Int
    ) {

        viewModelScope.launch {

            try {

                _isLoading.value = true
                _errorMessage.value = null

                coroutineScope {

                    val detailsDeferred = async {
                        repository.getIncidentDetails(incidentId)
                    }

                    val impactDeferred = async {
                        repository.getIncidentImpactDetails(incidentId)
                    }

                    _incidentDetails.value =
                        detailsDeferred.await()

                    _impactAssets.value =
                        impactDeferred.await()
                }

            } catch (e: Exception) {

                e.printStackTrace()
                _errorMessage.value =
                    e.message ?: "Failed to load incident details"

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun DownloadIncidentReport(
        incidentId: Int,
        context: Context
    ) {

        viewModelScope.launch {

            try {
                _isLoading.value = true
                val response =
                    repository.downloadIncidentReport(incidentId)

                if (response.isSuccessful) {

                    response.body()?.let {

                        val savedPdf = savePdf(
                            context,
                            it,
                            "Incident_$incidentId.pdf"
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

    fun addComment(
        incidentId: Int,
        commentText: String,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            try {

                _isPostingComment.value = true

                val json = """
                {
                    "commentText": "$commentText"
                }
            """.trimIndent()

                val requestBody = json.toRequestBody(
                    "application/json".toMediaType()
                )

                val success = repository.addIncidentComment(
                    incidentId,
                    requestBody
                )

                if (success) {
                    onSuccess()
                    loadIncidentDetails(incidentId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isPostingComment.value = false
            }
        }
    }

}