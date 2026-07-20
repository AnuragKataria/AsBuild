//package com.rbt.survey.ui.assetManagement
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.rbt.survey.data.repository.AssetRepository
//
//class AssetDetailViewModelFactory(
//    private val repository: AssetRepository
//) : ViewModelProvider.Factory {
//
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(AssetDetailViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return AssetDetailViewModel(repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}