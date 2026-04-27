package com.rbt.survey.dgps

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothDgpsConnector(private val context: Context) {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var job: Job? = null

    val status = MutableStateFlow<DgpsStatus>(DgpsStatus.Idle)
    val location = MutableStateFlow<DgpsLocation?>(null)
    val rawNmea = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    
    private val nmeaParser = NmeaParser()

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                status.value = DgpsStatus.Connecting
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                status.value = DgpsStatus.Connected

                val inputStream = socket!!.inputStream
                val buffer = ByteArray(1024)
                val lineBuffer = StringBuilder()

                while (isActive) {
                    val bytes = inputStream.read(buffer)
                    if (bytes == -1) break
                    
                    val chunk = String(buffer, 0, bytes)
                    lineBuffer.append(chunk)
                    
                    var newlineIndex: Int
                    while (lineBuffer.indexOf("\n").also { newlineIndex = it } >= 0) {
                        val line = lineBuffer.substring(0, newlineIndex).trim()
                        lineBuffer.delete(0, newlineIndex + 1)
                        
                        if (line.isNotEmpty()) {
                            rawNmea.tryEmit(line)
                            val loc = nmeaParser.parse(line)
                            if (loc != null) {
                                location.value = loc
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                status.value = DgpsStatus.Error(e.message ?: "Unknown error")
                e.printStackTrace()
            } finally {
                disconnect()
            }
        }
    }

    fun sendCorrectionData(data: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket?.outputStream?.write(data)
                socket?.outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        job?.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        socket = null
        status.value = DgpsStatus.Idle
    }
}
