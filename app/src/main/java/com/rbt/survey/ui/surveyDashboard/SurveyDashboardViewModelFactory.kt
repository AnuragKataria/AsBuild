package com.rbt.survey.ui.surveyDashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.repository.FormRepository
import com.rbt.survey.data.repository.GeoRepository

class SurveyDashboardViewModelFactory(
    private val repository: FormRepository,
    private val geoRepository: GeoRepository,
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SurveyDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SurveyDashboardViewModel(repository, geoRepository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
