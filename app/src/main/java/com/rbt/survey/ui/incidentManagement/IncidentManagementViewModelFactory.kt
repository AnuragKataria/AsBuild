package com.rbt.survey.ui.incidentManagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.repository.AssetRepository


class IncidentManagementViewModelFactory(
    private val repository: AssetRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncidentManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IncidentManagementViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}