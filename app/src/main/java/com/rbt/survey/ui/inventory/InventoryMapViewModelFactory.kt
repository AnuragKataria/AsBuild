package com.rbt.survey.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbt.survey.data.repository.AssetRepository

class InventoryMapViewModelFactory(
    private val repository: AssetRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryMapViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}