package com.rbt.survey.location

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import android.util.Log
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.local.db.AppDatabase
import com.rbt.survey.data.remote.RetrofitClient
import com.rbt.survey.data.repository.GeoRepository
import com.rbt.survey.data.model.LocationRequest
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.rbt.survey.data.local.db.LocationEntity
import com.rbt.survey.data.repository.toRequest
import com.rbt.survey.data.utils.isInternetAvailable

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var job: Job? = null
    private lateinit var preferences: UserPreferences
    private lateinit var geoRepository: GeoRepository

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        preferences = UserPreferences(this)

        val database = AppDatabase.getDatabase(this)

        val geoApi = RetrofitClient.getGeoApi(this, preferences)

        geoRepository = GeoRepository(
            geoApi,
            database.cachedBlockAssignmentDao(),
            database.cachedBlockSummaryDao(),
            database.cachedUploadedSubmissionDao(),
            database.locationDao()
        )

        startForeground(1, createNotification())

        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 🔥 CORE LOGIC
    private fun startLocationUpdates() {
        job = CoroutineScope(Dispatchers.IO).launch {

            while (true) {
                try {
                    val location = getCurrentLocation()

                    location?.let {
                        sendLocationToServer(it)
                    }

                } catch (e: Exception) {
                    Log.e("LocationService", "Error: ${e.message}")
                }

                delay(60000) // every 60 seconds
//                delay(300000) // every 60 seconds
            }
        }
    }

    //  Get Location
    private suspend fun getCurrentLocation(): Location? {

        // Permission check
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.e("LocationService", "Location permission not granted")
            return null
        }

        return suspendCancellableCoroutine { cont ->

            try {
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()

                fusedLocationClient.getCurrentLocation(request, null)
                    .addOnSuccessListener { location ->
                        cont.resume(location, null)
                    }
                    .addOnFailureListener {
                        cont.resume(null, null)
                    }

            } catch (e: SecurityException) {
                Log.e("LocationService", "Permission error: ${e.message}")
                cont.resume(null, null)
            }
        }
    }

    // 🌐 API CALL (you will connect your API here)
    private fun sendLocationToServer(location: Location) {

        CoroutineScope(Dispatchers.IO).launch {

            try {
                val userId = preferences.userId.first()?.toIntOrNull() ?: return@launch
                val userName = preferences.userName.first() ?: ""
                val email = preferences.userEmail.first() ?: ""

                val request = LocationEntity(
                    userId = userId,
                    userName = userName,
                    email = email,
                    lat = location.latitude,
                    lng = location.longitude,
                    accuracy = location.accuracy.toDouble(),
                    altitude = location.altitude,
                    speed = location.speed.toDouble(),
                    heading = location.bearing.toDouble(),
                    deviceType = "Android",
                    recordedAt = Instant.now().toString()
                )

                if (isInternetAvailable(applicationContext)) {

                    // 🟢 Send current location (same as before)
                    val success = geoRepository.sendLocation(request.toRequest(includeRecordedAt = false))

                    if (success) {
                        Log.d("LocationService", "Location sent successfully")
                    } else {
                        Log.e("LocationService", "Failed to send location")
                    }

                    // Check and sync old data
                    syncPendingLocations()

                } else {
                    // 🔴 Offline → store in DB
                    geoRepository.saveLocationOffline(request)
                }


//                val success = geoRepository.sendLocation(request)
//
//                if (success) {
//                    Log.d("LocationService", "Location sent successfully")
//                } else {
//                    Log.e("LocationService", "Failed to send location")
//                }

            } catch (e: Exception) {
                Log.e("LocationService", "Error sending location: ${e.message}")
            }
        }
    }

    private suspend fun syncPendingLocations() {

        val pendingList = geoRepository.getPendingLocations()

        if (pendingList.isEmpty()) return

        for (item in pendingList) {

            try {
                geoRepository.sendLocation(item.toRequest(includeRecordedAt = true))
            } catch (e: Exception) {
                // ignore error
            }

            // ALWAYS delete (your requirement)
            geoRepository.deleteLocation(item)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "location_tracking_channel"

        val channel = NotificationChannel(
            channelId,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Tracking Active")
            .setContentText("Your location is being tracked")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf() // STOP when app removed
    }
}