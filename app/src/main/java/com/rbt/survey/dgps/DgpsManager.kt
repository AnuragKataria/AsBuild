package com.rbt.survey.dgps

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DgpsManager(context: Context) {
    private val bluetoothConnector = BluetoothDgpsConnector(context)
    private val ntripClient = NtripClient()
    
    val status = bluetoothConnector.status
    val location = bluetoothConnector.location
    val isNtripConnected = ntripClient.isConnected

    private var ntripJob: Job? = null

    fun connect(
        deviceAddress: String,
        corsHost: String?,
        corsPort: Int?,
        corsMountpoint: String?,
        corsUser: String?,
        corsPass: String?
    ) {
        bluetoothConnector.connect(deviceAddress)
        
        if (!corsHost.isNullOrEmpty() && corsPort != null && !corsMountpoint.isNullOrEmpty()) {
            startNtrip(corsHost, corsPort, corsMountpoint, corsUser ?: "", corsPass ?: "")
        }
    }

    private fun startNtrip(host: String, port: Int, mountpoint: String, user: String, pass: String) {
        ntripJob?.cancel()
        ntripJob = CoroutineScope(Dispatchers.IO).launch {
            ntripClient.connect(host, port, mountpoint, user, pass) { data ->
                bluetoothConnector.sendCorrectionData(data)
            }
        }
    }

    fun disconnect() {
        bluetoothConnector.disconnect()
        ntripClient.disconnect()
        ntripJob?.cancel()
    }

    suspend fun fetchMountpoints(host: String, port: Int): List<String> = 
        ntripClient.fetchMountpoints(host, port)
}
