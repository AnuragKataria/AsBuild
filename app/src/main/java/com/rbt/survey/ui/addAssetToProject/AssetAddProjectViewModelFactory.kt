//package com.rbt.survey.ui.addAssetToProject
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.rbt.survey.data.repository.AssetRepository
//
//class AssetAddProjectViewModelFactory(
//    private val repository: AssetRepository
//) : ViewModelProvider.Factory {
//
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(AssetAddProjectViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return AssetAddProjectViewModel(repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}