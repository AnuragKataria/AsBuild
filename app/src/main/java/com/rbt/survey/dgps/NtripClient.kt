package com.rbt.survey.dgps

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class NtripClient {
    private var socket: Socket? = null
    val isConnected = MutableStateFlow(false)

    suspend fun connect(
        host: String,
        port: Int,
        mountpoint: String,
        user: String,
        pass: String,
        onDataReceived: (ByteArray) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                socket = Socket(host, port)
                val outputStream = socket!!.getOutputStream()
                val inputStream = socket!!.getInputStream()

                val auth = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
                val request = "GET /$mountpoint HTTP/1.0\r\n" +
                        "User-Agent: NTRIP AndroidClient/1.0\r\n" +
                        "Authorization: Basic $auth\r\n" +
                        "Connection: close\r\n\r\n"

                outputStream.write(request.toByteArray())
                outputStream.flush()

                // Skip HTTP response header
                val buffer = ByteArray(1024)
                var bytesRead: Int
                
                isConnected.value = true
                
                while (isConnected.value) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    
                    val data = buffer.copyOf(bytesRead)
                    onDataReceived(data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                disconnect()
            }
        }
    }

    fun disconnect() {
        isConnected.value = false
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        socket = null
    }

    suspend fun fetchMountpoints(host: String, port: Int): List<String> {
        return withContext(Dispatchers.IO) {
            val mountpoints = mutableListOf<String>()
            var tempSocket: Socket? = null
            try {
                tempSocket = Socket(host, port)
                tempSocket.soTimeout = 5000
                val outputStream = tempSocket.getOutputStream()
                val inputStream = tempSocket.getInputStream()

                val request = "GET / HTTP/1.0\r\n" +
                        "User-Agent: NTRIP AndroidClient/1.0\r\n" +
                        "Connection: close\r\n\r\n"

                outputStream.write(request.toByteArray())
                outputStream.flush()

                val reader = inputStream.bufferedReader()
                var line: String? = reader.readLine()
                
                // Skip HTTP headers
                while (line != null && line.isNotEmpty()) {
                    line = reader.readLine()
                }

                // Read source table
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("STR")) {
                        val parts = line!!.split(";")
                        if (parts.size > 1) {
                            mountpoints.add(parts[1])
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    tempSocket?.close()
                } catch (e: Exception) {}
            }
            mountpoints
        }
    }
}
