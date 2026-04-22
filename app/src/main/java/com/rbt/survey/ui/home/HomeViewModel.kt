package com.rbt.survey.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.model.FormData
import com.rbt.survey.data.repository.FormRepository
import com.rbt.survey.data.model.Assignment
import com.rbt.survey.data.model.BlockSummary
import com.rbt.survey.data.model.GpItem
import com.rbt.survey.data.model.SubmissionItem
import com.rbt.survey.data.model.SubmissionSearchRequest
import com.rbt.survey.data.local.db.OfflineSubmission
import com.rbt.survey.data.repository.GeoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val forms: List<FormData>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: FormRepository,
    private val geoRepository: GeoRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _blockAssignments = MutableStateFlow<List<Assignment>>(emptyList())
    val blockAssignments: StateFlow<List<Assignment>> = _blockAssignments.asStateFlow()

    private val _selectedBlockCode = MutableStateFlow<String?>(null)
    val selectedBlockCode: StateFlow<String?> = _selectedBlockCode.asStateFlow()

    private val _blockSummaries = MutableStateFlow<List<BlockSummary>>(emptyList())
    val blockSummaries: StateFlow<List<BlockSummary>> = _blockSummaries.asStateFlow()

    private val _blockSummaryLoading = MutableStateFlow(false)
    val blockSummaryLoading: StateFlow<Boolean> = _blockSummaryLoading.asStateFlow()
    private val _selectedForm = MutableStateFlow<FormData?>(null)
    val selectedForm: StateFlow<FormData?> = _selectedForm

    private val _selectedGpStatusList = MutableStateFlow<List<GpItem>>(emptyList())
    val selectedGpStatusList: StateFlow<List<GpItem>> = _selectedGpStatusList

//    private val _completedSelectedForm = MutableStateFlow<FormData?>(null)
//    val completedSelectedForm: StateFlow<FormData?> = _completedSelectedForm

    private val _completedBlockSummaries = MutableStateFlow<List<BlockSummary>>(emptyList())
    val completedBlockSummaries: StateFlow<List<BlockSummary>> = _completedBlockSummaries

    private val _completedLoading = MutableStateFlow(false)
    val completedLoading: StateFlow<Boolean> = _completedLoading

    private val _uploadedItems = MutableStateFlow<List<SubmissionItem>>(emptyList())
    val uploadedItems: StateFlow<List<SubmissionItem>> = _uploadedItems

    private val _uploadedLoading = MutableStateFlow(false)
    val uploadedLoading: StateFlow<Boolean> = _uploadedLoading

    private val _offlineSubmissions = MutableStateFlow<List<OfflineSubmission>>(emptyList())
    val offlineSubmissions: StateFlow<List<OfflineSubmission>> = _offlineSubmissions

    private val _offlineLoading = MutableStateFlow(false)
    val offlineLoading: StateFlow<Boolean> = _offlineLoading

//    private val _uploadedSelectedForm = MutableStateFlow<FormData?>(null)
//    val uploadedSelectedForm: StateFlow<FormData?> = _uploadedSelectedForm

    init {
        fetchForms()
        fetchBlockAssignments()
    }

//    fun selectUploadedForm(form: FormData) {
//        _uploadedSelectedForm.value = form
//    }

    fun setSelectedGpStatusList(list: List<GpItem>) {
        _selectedGpStatusList.value = list
    }

    fun selectForm(form: FormData) {
        _selectedForm.value = form
    }

//    fun selectCompletedForm(form: FormData) {
//        _completedSelectedForm.value = form
//    }

    fun fetchCompletedBlockSummary(formId: Int) {
        viewModelScope.launch {
            _completedLoading.value = true
            try {
                val userIdStr = preferences.userId.first() ?: return@launch
                val response = geoRepository.getBlockSummary(userIdStr, formId)

                if (response.isSuccessful && response.body()?.success == true) {
                    _completedBlockSummaries.value = response.body()?.data ?: emptyList()
                } else {
                    _completedBlockSummaries.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _completedBlockSummaries.value = emptyList()
            } finally {
                _completedLoading.value = false
            }
        }
    }

    fun fetchUploadedSubmissions(formId: Int) {
        viewModelScope.launch {
            _uploadedLoading.value = true
            try {
                val response = geoRepository.getUploadedSubmissions(
                    formId,
                    SubmissionSearchRequest()
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    _uploadedItems.value = response.body()?.data?.items ?: emptyList()
                } else {
                    _uploadedItems.value = emptyList()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uploadedItems.value = emptyList()
            } finally {
                _uploadedLoading.value = false
            }
        }
    }

    fun fetchOfflineSubmissions(formId: Int) {
        viewModelScope.launch {
            _offlineLoading.value = true
            try {
                _offlineSubmissions.value = repository.getOfflineSubmissions(formId)
            } catch (e: Exception) {
                e.printStackTrace()
                _offlineSubmissions.value = emptyList()
            } finally {
                _offlineLoading.value = false
            }
        }
    }

    fun fetchForms() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val response = repository.getForms()
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = HomeUiState.Success(response.body()?.data ?: emptyList())
                } else {
                    _uiState.value = HomeUiState.Error(response.body()?.message ?: "Failed to load forms")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    private fun fetchBlockAssignments() {
        viewModelScope.launch {
            try {
                val userIdStr = preferences.userId.first() ?: return@launch
                val response = geoRepository.getBlockAssignments(userIdStr)
                if (response.isSuccessful && response.body()?.success == true) {
                    val assignments = response.body()?.data?.assignments ?: emptyList()
                    _blockAssignments.value = assignments
                    if (assignments.isNotEmpty() && _selectedBlockCode.value == null) {
                        _selectedBlockCode.value = assignments.first().blockCode
                    }
                }
            } catch (e: Exception) {
                // Silently fail for now or handle as needed
            }
        }
    }

    fun fetchBlockSummary(formId: Int) {
        viewModelScope.launch {
            _blockSummaryLoading.value = true
            try {
                val userIdStr = preferences.userId.first() ?: return@launch
                val response = geoRepository.getBlockSummary(userIdStr, formId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _blockSummaries.value = response.body()?.data ?: emptyList()
                } else {
                    _blockSummaries.value = emptyList()
                }
            } catch (e: Exception) {
                _blockSummaries.value = emptyList()
            } finally {
                _blockSummaryLoading.value = false
            }
        }
    }

    fun onBlockCodeSelected(blockCode: String) {
        _selectedBlockCode.value = blockCode
    }

    fun logout() {
        viewModelScope.launch {
            preferences.clearAuthData()
        }
    }

    fun uploadSubmission(context: android.content.Context, submissionId: Int) {
        viewModelScope.launch {
            _offlineLoading.value = true
            try {
                val submission = repository.getOfflineSubmissionById(submissionId)
                if (submission != null) {
                    val requestBody = submission.submissionData.toRequestBody("application/json".toMediaType())
                    val response = repository.submitForm(submission.formId, requestBody)
                    
                    if (response.isSuccessful) {
                        repository.deleteOfflineSubmission(submissionId)
                        android.widget.Toast.makeText(context, "Uploaded successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        fetchOfflineSubmissions(submission.formId)
                    } else {
                        android.widget.Toast.makeText(context, "Upload failed: ${response.code()}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Exception: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                _offlineLoading.value = false
            }
        }
    }
}
