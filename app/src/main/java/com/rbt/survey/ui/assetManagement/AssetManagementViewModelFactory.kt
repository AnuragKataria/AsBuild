package com.rbt.survey.ui.assetManagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.repository.AssetRepository

class AssetManagementViewModelFactory(
    private val repository: AssetRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssetManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssetManagementViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}