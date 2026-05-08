package com.rbt.survey.ui.locationTrackingDashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.repository.GeoRepository

class LocationTrackingViewModelFactory(
    private val repository: GeoRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(LocationTrackingViewModel::class.java)) {

            return LocationTrackingViewModel(
                repository
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}