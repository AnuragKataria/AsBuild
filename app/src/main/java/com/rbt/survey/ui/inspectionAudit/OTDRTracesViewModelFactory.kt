package com.rbt.survey.ui.inspectionAudit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.repository.AssetRepository
import com.rbt.survey.ui.incidentManagement.IncidentDetailsViewModel


class OTDRTracesViewModelFactory(
    private val repository: AssetRepository,
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OTDRTracesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OTDRTracesViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}