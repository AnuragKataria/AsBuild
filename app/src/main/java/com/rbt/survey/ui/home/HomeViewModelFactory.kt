package com.rbt.survey.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.repository.FormRepository
import com.rbt.survey.data.repository.GeoRepository

class HomeViewModelFactory(
    private val repository: FormRepository,
    private val geoRepository: GeoRepository,
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, geoRepository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
