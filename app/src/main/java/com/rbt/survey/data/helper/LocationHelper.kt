package com.rbt.survey.data.helper

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationHelper(private val context: Context) {

    private val fusedClient =
        com.google.android.gms.location.LocationServices
            .getFusedLocationProviderClient(context)

    fun isLocationEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager

        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): android.location.Location? {
        return suspendCancellableCoroutine { cont ->

            val request = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                0
            ).apply {
                setWaitForAccurateLocation(true)
                setMaxUpdates(1)
            }.build()

            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    fusedClient.removeLocationUpdates(this)
                    cont.resume(result.lastLocation, null)
                }
            }

            fusedClient.requestLocationUpdates(
                request,
                callback,
                android.os.Looper.getMainLooper()
            )
        }
    }
}