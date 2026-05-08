package com.rbt.survey.ui.locationTrackingDashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.model.LatestLocationResponse
import com.rbt.survey.data.model.UserLocationItem
import com.rbt.survey.data.repository.GeoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationTrackingViewModel(
    private val geoRepository: GeoRepository
) : ViewModel() {

    // LOADING
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // USERS DROPDOWN LIST
    private val _usersList =
        MutableStateFlow<List<UserLocationItem>>(emptyList())

    val usersList: StateFlow<List<UserLocationItem>> =
        _usersList.asStateFlow()

    // ALL USERS LOCATION
    private val _allUsersLocations =
        MutableStateFlow<List<UserLocationItem>>(emptyList())

    val allUsersLocations: StateFlow<List<UserLocationItem>> =
        _allUsersLocations.asStateFlow()

    // SINGLE USER LATEST LOCATION
    private val _latestUserLocation =
        MutableStateFlow<LatestLocationResponse?>(null)

    val latestUserLocation: StateFlow<LatestLocationResponse?> =
        _latestUserLocation.asStateFlow()

    // USER HISTORY
    private val _historyLocations =
        MutableStateFlow<List<UserLocationItem>>(emptyList())

    val historyLocations: StateFlow<List<UserLocationItem>> =
        _historyLocations.asStateFlow()

    // ERROR
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _emptyMessage =
        MutableStateFlow<String?>(null)

    val emptyMessage: StateFlow<String?> =
        _emptyMessage.asStateFlow()

    // =========================================
    // ALL USERS
    // =========================================

    fun getAllUsersLocations(showOnMap: Boolean) {

        viewModelScope.launch {

            _loading.value = true

            try {

                val response = geoRepository.getAllUsersLocations()

                if (response.isSuccessful) {

                    val items = response.body()?.items ?: emptyList()

                    // FOR MAP
                    if(showOnMap) {
                        _allUsersLocations.value = items
                    }

                    // FOR DROPDOWN
                    _usersList.value = items

                    if (showOnMap && items.isEmpty()) {

                        _emptyMessage.value =
                            "No users location found"
                    }

                } else {

                    _error.value =
                        "Failed to fetch all users location"
                }

            } catch (e: Exception) {

                _error.value =
                    e.localizedMessage ?: "Unknown error"
            }

            _loading.value = false
        }
    }

    // =========================================
    // LATEST USER LOCATION
    // =========================================

    fun getLatestUserLocation(
        userId: Int
    ) {

        viewModelScope.launch {

            _loading.value = true

            try {

                val response =
                    geoRepository.getLatestUserLocation(userId)

                if (response.isSuccessful) {

                    _latestUserLocation.value =
                        response.body()

                    if (response.body() == null) {

                        _emptyMessage.value =
                            "No latest location found"
                    }

                } else {

                    _error.value =
                        "Failed to fetch latest location"
                }

            } catch (e: Exception) {

                _error.value =
                    e.localizedMessage ?: "Unknown error"
            }

            _loading.value = false
        }
    }

    // =========================================
    // USER HISTORY
    // =========================================

    fun getUserLocationHistory(
        userId: Int,
        from: String,
        to: String
    ) {

        viewModelScope.launch {

            _loading.value = true

            try {

                val response =
                    geoRepository.getUserLocationHistory(
                        userId,
                        from,
                        to
                    )

                if (response.isSuccessful) {

                    val items =
                        response.body()?.items ?: emptyList()

                    _historyLocations.value = items

                    if (items.isEmpty()) {

                        _emptyMessage.value =
                            "No history found"
                    }

                } else {

                    _error.value =
                        "Failed to fetch history"
                }

            } catch (e: Exception) {

                _error.value =
                    e.localizedMessage ?: "Unknown error"
            }

            _loading.value = false
        }
    }

    // =========================================
    // CLEAR
    // =========================================

    fun clearData() {

        _allUsersLocations.value = emptyList()

        _latestUserLocation.value = null

        _historyLocations.value = emptyList()
    }

    fun clearToastMessage() {

        _emptyMessage.value = null
    }
}