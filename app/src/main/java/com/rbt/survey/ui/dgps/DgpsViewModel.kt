package com.rbt.survey.ui.dgps

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.dgps.DgpsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DgpsViewModel(
    private val userPreferences: UserPreferences,
    private val dgpsManager: DgpsManager
) : ViewModel() {

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bluetoothDevices = _bluetoothDevices.asStateFlow()

    val dgpsStatus = dgpsManager.status
    val dgpsLocation = dgpsManager.location
    val isNtripConnected = dgpsManager.isNtripConnected

    private val _mountpoints = MutableStateFlow<List<String>>(emptyList())
    val mountpoints = _mountpoints.asStateFlow()

    private val _isFetchingMountpoints = MutableStateFlow(false)
    val isFetchingMountpoints = _isFetchingMountpoints.asStateFlow()

    val savedAddress = userPreferences.dgpsDeviceAddress
    val savedHost = userPreferences.corsHost
    val savedPort = userPreferences.corsPort
    val savedMountpoint = userPreferences.corsMountpoint
    val savedUser = userPreferences.corsUser
    val savedPass = userPreferences.corsPass
    val useDgps = userPreferences.useDgps
    val useCors = userPreferences.useCors

    @SuppressLint("MissingPermission")
    fun scanDevices() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            _bluetoothDevices.value = adapter.bondedDevices.toList()
        }
    }

    fun saveAndConnect(
        address: String,
        host: String,
        port: String,
        mountpoint: String,
        user: String,
        pass: String,
        enabled: Boolean,
        useCorsFlag: Boolean
    ) {
        viewModelScope.launch {
            userPreferences.saveDgpsSettings(address, host, port, mountpoint, user, pass, enabled, useCorsFlag)
            if (enabled) {
                dgpsManager.connect(
                    address, 
                    if (useCorsFlag) host else null, 
                    if (useCorsFlag) port.toIntOrNull() else null, 
                    if (useCorsFlag) mountpoint else null, 
                    if (useCorsFlag) user else null, 
                    if (useCorsFlag) pass else null
                )
            } else {
                dgpsManager.disconnect()
            }
        }
    }

    fun disconnect() {
        dgpsManager.disconnect()
    }

    fun fetchMountpoints(host: String, port: String) {
        val p = port.toIntOrNull() ?: return
        viewModelScope.launch {
            _isFetchingMountpoints.value = true
            try {
                _mountpoints.value = dgpsManager.fetchMountpoints(host, p)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isFetchingMountpoints.value = false
            }
        }
    }
}

class DgpsViewModelFactory(
    private val userPreferences: UserPreferences,
    private val dgpsManager: DgpsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DgpsViewModel(userPreferences, dgpsManager) as T
    }
}
