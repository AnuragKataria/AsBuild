package com.rbt.survey.dgps

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DgpsManager(context: Context) {
    private val bluetoothConnector = BluetoothDgpsConnector(context)
    private val ntripClient = NtripClient()
    
    val status = bluetoothConnector.status
    val location = bluetoothConnector.location
    val isNtripConnected = ntripClient.isConnected

    init {
        CoroutineScope(Dispatchers.IO).launch {
            bluetoothConnector.rawNmea.collect { nmea ->
                if (nmea.contains("GGA")) {
                    ntripClient.sendNmea(nmea)
                }
            }
        }
    }

    private var ntripJob: Job? = null

    fun connectBluetooth(deviceAddress: String) {
        bluetoothConnector.connect(deviceAddress)
    }

    fun disconnectBluetooth() {
        disconnectNtrip()
        bluetoothConnector.disconnect()
    }

    fun connectNtrip(
        host: String,
        port: Int,
        mountpoint: String,
        user: String,
        pass: String
    ) {
        ntripJob?.cancel()
        ntripJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    ntripClient.connect(host, port, mountpoint, user, pass) { data ->
                        bluetoothConnector.sendCorrectionData(data)
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    e.printStackTrace()
                    
                    // Stop retrying if it's an authentication error
                    if (e.message?.contains("401") == true) {
                        break
                    }
                    
                    // If it was a timeout or connection issue, wait and retry
                    delay(5000) 
                }
            }
        }
    }

    fun disconnectNtrip() {
        ntripJob?.cancel()
        ntripJob = null
        ntripClient.disconnect()
    }

    fun connect(
        deviceAddress: String,
        corsHost: String?,
        corsPort: Int?,
        corsMountpoint: String?,
        corsUser: String?,
        corsPass: String?
    ) {
        connectBluetooth(deviceAddress)
        
        if (!corsHost.isNullOrEmpty() && corsPort != null && !corsMountpoint.isNullOrEmpty()) {
            connectNtrip(corsHost, corsPort, corsMountpoint, corsUser ?: "", corsPass ?: "")
        }
    }

    fun disconnect() {
        disconnectBluetooth()
    }

    suspend fun fetchMountpoints(host: String, port: Int): List<String> = 
        ntripClient.fetchMountpoints(host, port)
}
