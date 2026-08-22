package com.rbt.survey.ui.incidentManagement

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.model.*
import com.rbt.survey.data.repository.AssetRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class IncidentManagementViewModel(
    private val repository: AssetRepository
) : ViewModel() {


    private val _totalTicketCount = MutableStateFlow(0)
    val totalTicketCount: StateFlow<Int> = _totalTicketCount

    private val _criticalCount = MutableStateFlow(0)
    val criticalCount: StateFlow<Int> = _criticalCount

    private val _inProgressCount = MutableStateFlow(0)
    val inProgressCount: StateFlow<Int> = _inProgressCount

    private val _resolvedCount = MutableStateFlow(0)
    val resolvedCount: StateFlow<Int> = _resolvedCount

    private val _incidents =
        MutableStateFlow<List<IncidentItem>>(emptyList())

    val incidents: StateFlow<List<IncidentItem>> = _incidents

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _pageSize = MutableStateFlow(10)
    val pageSize: StateFlow<Int> = _pageSize

    private val _totalRecords = MutableStateFlow(0)
    val totalRecords: StateFlow<Int> = _totalRecords

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filter = MutableStateFlow(IncidentFilter())
    val filter: StateFlow<IncidentFilter> = _filter.asStateFlow()

    private val _projects = MutableStateFlow<List<ProjectResponse>>(emptyList())
    val projects: StateFlow<List<ProjectResponse>> = _projects.asStateFlow()

    private val _selectedprojectassets = MutableStateFlow<List<CreatedAssetData>>(emptyList())
    val selectedprojectassets: StateFlow<List<CreatedAssetData>> = _selectedprojectassets

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingAssets = MutableStateFlow(false)
    val isLoadingAssets: StateFlow<Boolean> = _isLoadingAssets

    private val _isLoggingIncident = MutableStateFlow(false)
    val isLoggingIncident: StateFlow<Boolean> = _isLoggingIncident

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadIncidentScreen() {
        loadDashboardCounts()
        loadIncidentPage(1)
        loadIncidentFilterProjects()
    }

    fun loadDashboardCounts() {

        viewModelScope.launch {

            try {

                coroutineScope {

                    val totalDeferred = async {
                        repository.getIncidents(
                            pageSize = 1
                        )
                    }

                    val criticalDeferred = async {
                        repository.getIncidents(
                            priority = "CRITICAL",
                            pageSize = 1
                        )
                    }

                    val inProgressDeferred = async {
                        repository.getIncidents(
                            status = "IN_PROGRESS",
                            pageSize = 1
                        )
                    }

                    val resolvedDeferred = async {
                        repository.getIncidents(
                            status = "RESOLVED",
                            pageSize = 1
                        )
                    }

                    val totalResponse = totalDeferred.await()
                    val criticalResponse = criticalDeferred.await()
                    val inProgressResponse = inProgressDeferred.await()
                    val resolvedResponse = resolvedDeferred.await()

                    _totalTicketCount.value =
                        totalResponse.totalCount

                    _criticalCount.value =
                        criticalResponse.totalCount

                    _inProgressCount.value =
                        inProgressResponse.totalCount

                    _resolvedCount.value =
                        resolvedResponse.totalCount
                }

            } catch (e: Exception) {

                _error.value =
                    e.message ?: "Failed to load dashboard counts"
            }
        }
    }

    fun loadIncidentPage(pageNumber: Int = 1) {

        viewModelScope.launch {

            _isLoading.value = true
            _error.value = null

            try {

                val currentFilter = _filter.value

                val response = repository.getIncidents(
                    projectId = currentFilter.projectId,
                    status = currentFilter.status,
                    priority = currentFilter.priority,
                    category = currentFilter.category,
                    assetId = currentFilter.assetId,
                    assigneeId = currentFilter.assigneeId,
                    searchQuery = _searchQuery.value
                        .takeIf { it.isNotBlank() },
                    pageNumber = pageNumber,
                    pageSize = _pageSize.value
                )

                _incidents.value = response.items

                _totalRecords.value = response.totalCount

                _currentPage.value = response.pageNumber

            } catch (e: Exception) {

                _error.value =
                    e.message ?: "Failed to load incidents"

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun searchIncidents(query: String) {

        _searchQuery.value = query
    }

    init {

        viewModelScope.launch {

            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest {

                    loadIncidentPage(1)
                }
        }
    }

    fun updateIncidentFilter(
        projectId: Long? = _filter.value.projectId,
        status: String? = _filter.value.status,
        priority: String? = _filter.value.priority,
        category: String? = _filter.value.category,
        assetId: Long? = _filter.value.assetId,
        assigneeId: Long? = _filter.value.assigneeId
    ) {

        _filter.value = IncidentFilter(
            projectId = projectId,
            status = status,
            priority = priority,
            category = category,
            assetId = assetId,
            assigneeId = assigneeId,
            searchQuery = _searchQuery.value
                .takeIf { it.isNotBlank() }
        )
    }

    fun clearIncidentFilter() {

        _filter.value = IncidentFilter()
    }

    fun loadIncidentFilterProjects() {

        viewModelScope.launch {

            try {
                _projects.value = repository.getProjects()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load projects"
            }
        }
    }

    fun loadAssets(projectId: Int) {

        viewModelScope.launch {

            _selectedprojectassets.value = emptyList()
            _isLoadingAssets.value = true

            try {

                val response = repository.getCreatedAssets(projectId)

                if (response.success) {

                    _selectedprojectassets.value = response.data

                } else {

                    _selectedprojectassets.value = emptyList()

                    _error.value =
                        response.message ?: "Failed to load assets"
                }

            } catch (e: Exception) {

                _selectedprojectassets.value = emptyList()

                _error.value =
                    e.message ?: "Failed to load assets"

            } finally {

                _isLoadingAssets.value = false
            }
        }
    }

    fun logIncident(
        context: Context,
        projectId: Long,
        assetId: Long?,
        title: String,
        category: String,
        priority: String,
        description: String,
        latitude: Double,
        longitude: Double,
        attachments: List<Uri>?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            _isLoggingIncident.value = true

            try {

                val response = repository.logIncident(
                    context = context,
                    projectId = projectId,
                    assetId = assetId,
                    title = title,
                    category = category,
                    priority = priority,
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    attachments = attachments
                )

                Log.d(
                    "REPORT_INCIDENT",
                    "Response = ${response.body()}"
                )

                if (response.isSuccessful) {

                    val incidentId = response.body()

                    if (incidentId != null) {

                        onSuccess()

                    } else {

                        onError("Failed to create incident")
                    }

                } else {

                    onError(
                        response.message().ifBlank {
                            "Failed to create incident"
                        }
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "REPORT_INCIDENT",
                    "Error = ${e.message}",
                    e
                )

                onError(
                    e.message ?: "Failed to create incident"
                )
            } finally {

                _isLoggingIncident.value = false
            }
        }
    }

}