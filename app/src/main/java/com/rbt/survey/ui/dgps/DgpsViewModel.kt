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

    fun selectBluetoothDevice(address: String) {
        viewModelScope.launch {
            userPreferences.saveDgpsSettings(
                address = address,
                host = null,
                port = null,
                mountpoint = null,
                user = null,
                pass = null,
                useDgps = true,
                useCors = true // keep as is
            )
            dgpsManager.connectBluetooth(address)
        }
    }

    fun disconnectBluetooth() {
        dgpsManager.disconnectBluetooth()
    }

    fun connectCors(
        host: String,
        port: String,
        mountpoint: String,
        user: String,
        pass: String
    ) {
        viewModelScope.launch {
            try {
                val p = port.toIntOrNull() ?: return@launch
                dgpsManager.connectNtrip(host, p, mountpoint, user, pass)
                
                // Save CORS settings
                userPreferences.saveDgpsSettings(
                    address = null,
                    host = host,
                    port = port,
                    mountpoint = mountpoint,
                    user = user,
                    pass = pass,
                    useDgps = true,
                    useCors = true
                )
            } catch (e: Exception) {
                if (e.message?.contains("401") == true) {
                    _errorFlow.emit("CORS Authentication Failed (401). Check username/password.")
                } else {
                    _errorFlow.emit("CORS Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun disconnectCors() {
        dgpsManager.disconnectNtrip()
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
            if (useCorsFlag && enabled) {
                val p = port.toIntOrNull() ?: return@launch
                dgpsManager.connectNtrip(host, p, mountpoint, user, pass)
            } else {
                dgpsManager.disconnectNtrip()
            }
        }
    }

    fun disconnect() {
        dgpsManager.disconnect()
    }

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    fun fetchMountpoints(host: String, port: String) {
        val p = port.toIntOrNull() ?: return
        viewModelScope.launch {
            _isFetchingMountpoints.value = true
            try {
                val results = dgpsManager.fetchMountpoints(host, p)
                if (results.isEmpty()) {
                    _errorFlow.emit("No mountpoints found or host unreachable")
                } else {
                    _mountpoints.value = results
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorFlow.emit("Connection error: ${e.localizedMessage}")
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
