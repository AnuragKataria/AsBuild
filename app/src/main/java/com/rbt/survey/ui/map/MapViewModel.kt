package com.rbt.survey.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.compose.MapType
import com.rbt.survey.data.model.GpItem
import com.rbt.survey.data.model.GpMapItem
import com.rbt.survey.data.model.GpStatus
import com.rbt.survey.data.repository.FormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: FormRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    private val _gpList = MutableStateFlow<List<GpMapItem>>(emptyList())
    val gpList: StateFlow<List<GpMapItem>> = _gpList

    private var completedGpSet: Set<String> = emptySet()

    private val _mapType = MutableStateFlow(MapType.NORMAL)
    val mapType: StateFlow<MapType> = _mapType

    fun setMapType(type: MapType) {
        _mapType.value = type
    }

    fun setCompletedGpList(gpList: List<GpItem>) {
        completedGpSet = gpList
            .filter { it.isCompleted }
            .map { it.lgdCode }
            .toSet()
    }

    fun loadGpLocations(formId: Int, blockCode: String?) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val response = repository.getFormDetail(formId, blockCode)

                if (response.isSuccessful && response.body()?.success == true) {

                    val formData = response.body()?.data!!

                    // ✅ GET GP FIELD FROM currentVersion
                    val gpField = formData.currentVersion
                        ?.schema
                        ?.fields
                        ?.find { it.id == "GP" }

                    // ✅ MAP OPTIONS TO GP LIST
                    val list = gpField?.options?.mapNotNull { option ->

                        val map = option as? Map<*, *> ?: return@mapNotNull null

                        val name = map["Label"]?.toString() ?: return@mapNotNull null
                        val raw = map["Raw"] as? Map<*, *> ?: return@mapNotNull null

                        val newLocation = raw["NewGpLocation"]?.toString()
                        val location = map["GpLocation"]?.toString()

                        var lat: Double? = null
                        var lng: Double? = null

                        // ✅ Case 1: Use NewGpLocation if present
                        if (!newLocation.isNullOrEmpty() && newLocation != "null") {
                            try {
                                val json = org.json.JSONObject(newLocation)
                                val coordinates = json.getJSONArray("coordinates")

                                // GeoJSON → [lng, lat]
                                lng = coordinates.getDouble(0)
                                lat = coordinates.getDouble(1)

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // ✅ Case 2: Else use GpLocation (JSON)
                        else if (!location.isNullOrEmpty()) {
                            try {
                                val json = org.json.JSONObject(location)
                                lat = json.optDouble("lat")
                                lng = json.optDouble("lng")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // ❌ Skip if invalid
                        if (lat == null || lng == null) return@mapNotNull null
                        val lgdCode = raw?.get("LgdCode")?.toString()

                        GpMapItem(
                            name = name,
                            lat = lat,
                            lng = lng,
                            lgdCode = lgdCode,
                            isCompleted = lgdCode != null && completedGpSet.contains(lgdCode)
                        )

                    } ?: emptyList()

                    _gpList.value = list
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            _isLoading.value = false
        }
    }
}