package com.rbt.survey.dgps

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class NtripClient {
    private var socket: Socket? = null
    private var outputStream: java.io.OutputStream? = null
    val isConnected = MutableStateFlow(false)

    fun sendNmea(nmea: String) {
        val os = outputStream
        if (isConnected.value && os != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    os.write(nmea.toByteArray())
                    os.write("\r\n".toByteArray())
                    os.flush()
                } catch (e: Exception) {
                    // Fail silently for NMEA feedback
                }
            }
        }
    }

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
                socket = Socket()
                socket?.connect(java.net.InetSocketAddress(host, port), 10000) // 10s connection timeout
                socket?.soTimeout = 120000 // 60s read timeout
                
                outputStream = socket!!.getOutputStream()
                val inputStream = socket!!.getInputStream()

                val auth = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
                val request = "GET /$mountpoint HTTP/1.0\r\n" +
                        "User-Agent: NTRIP AndroidClient/1.0\r\n" +
                        "Authorization: Basic $auth\r\n" +
                        "Connection: keep-alive\r\n\r\n"

                outputStream?.write(request.toByteArray())
                outputStream?.flush()

                // Read headers byte-by-byte to avoid buffering binary data
                val headerBuilder = StringBuilder()
                var consecutiveNewLines = 0
                while (true) {
                    val b = inputStream.read()
                    if (b == -1) break
                    val c = b.toChar()
                    headerBuilder.append(c)
                    
                    if (c == '\n') consecutiveNewLines++
                    else if (c != '\r') consecutiveNewLines = 0
                    
                    if (consecutiveNewLines >= 2) break // Found \r\n\r\n or \n\n
                }

                val fullHeader = headerBuilder.toString()
                if (!fullHeader.contains("200 OK") && !fullHeader.contains("ICY 200")) {
                    android.util.Log.e("NtripClient", "Connection rejected by server: $fullHeader")
                    isConnected.value = false
                    throw Exception("NTRIP Server rejected connection: $fullHeader")
                }

                isConnected.value = true
                val buffer = ByteArray(4096)
                var bytesRead: Int
                
                while (isConnected.value) {
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    
                    val data = buffer.copyOf(bytesRead)
                    onDataReceived(data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
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
        } finally {
            socket = null
            outputStream = null
        }
    }

    suspend fun fetchMountpoints(host: String, port: Int): List<String> {
        return withContext(Dispatchers.IO) {
            val mountpoints = mutableListOf<String>()
            var tempSocket: Socket? = null
            try {
                tempSocket = Socket()
                tempSocket.connect(java.net.InetSocketAddress(host, port), 5000) // 5s connection timeout
                tempSocket.soTimeout = 5000 // 5s read timeout
                
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
                throw e
            } finally {
                try {
                    tempSocket?.close()
                } catch (e: Exception) {}
            }
            mountpoints
        }
    }
}
