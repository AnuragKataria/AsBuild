//package com.rbt.survey.ui.addAssetToProject
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.rbt.survey.data.model.*
//import com.rbt.survey.data.repository.AssetRepository
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//
//class AssetAddProjectViewModel(
//    private val repository: AssetRepository
//) : ViewModel() {
//
//    private val _projects = MutableStateFlow<List<ProjectResponse>>(emptyList())
//    val projects: StateFlow<List<ProjectResponse>> = _projects
//
//    private val _states = MutableStateFlow<List<DropdownItem>>(emptyList())
//    val states: StateFlow<List<DropdownItem>> = _states
//
//    private val _districts = MutableStateFlow<List<DropdownItem>>(emptyList())
//    val districts: StateFlow<List<DropdownItem>> = _districts
//
//    private val _blocks = MutableStateFlow<List<DropdownItem>>(emptyList())
//    val blocks: StateFlow<List<DropdownItem>> = _blocks
//
//    private val _assetDetails = MutableStateFlow<List<AssetDetailResponse>>(emptyList())
//
//    val assetDetails: StateFlow<List<AssetDetailResponse>> = _assetDetails
//
//    private val _isLoading = MutableStateFlow(false)
//    val isLoading: StateFlow<Boolean> = _isLoading
//
//    private val _isAssetLoading = MutableStateFlow(false)
//    val isAssetLoading: StateFlow<Boolean> = _isAssetLoading
//
//    fun loadProjects() {
//        viewModelScope.launch {
//            try {
//                _isLoading.value = true
//                _projects.value = repository.getProjects()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun loadStates(
//        projectId: Int
//    ) {
//        viewModelScope.launch {
//
//            try {
//                _isLoading.value = true
//
//                _states.value =
//                    repository.getStates(projectId)
//                        .data
//                        ?.items
//                        ?: emptyList()
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun loadDistricts(
//        projectId: Int,
//        stateCodes: List<String>
//    ) {
//
//        viewModelScope.launch {
//
//            try {
//
//                _isLoading.value = true
//                _districts.value =
//                    repository.getDistricts(
//                        projectId,
//                        stateCodes
//                    ).data?.items ?: emptyList()
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun loadBlocks(
//        projectId: Int,
//        stateCodes: List<String>,
//        districtCodes: List<String>
//    ) {
//
//        viewModelScope.launch {
//
//            try {
//
//                _isLoading.value = true
//                _blocks.value =
//                    repository.getBlocks(
//                        projectId,
//                        stateCodes,
//                        districtCodes
//                    ).data?.items ?: emptyList()
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun loadAssets() {
//
//        viewModelScope.launch {
//            try {
//                _isAssetLoading.value = true
//
//                val assets = repository.getAssetTypes()
//
//                val details = assets.mapNotNull { asset ->
//                        try {
//                            repository.getAssetDetail(asset.assetTypeID)
//                        } catch (e: Exception) {
//                            null
//                        }
//                    }
//
//                _assetDetails.value = details
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                _isAssetLoading.value = false
//            }
//        }
//    }
//
//}