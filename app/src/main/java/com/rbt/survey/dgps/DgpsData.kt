package com.rbt.survey.dgps

data class DgpsLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val hrms: Float = 0f,
    val vrms: Float = 0f,
    val fixQuality: Int, // 0=Invalid, 1=GPS fix, 2=DGPS fix, 4=RTK fixed, 5=RTK float
    val satellites: Int,
    val satellitesList: List<SatelliteInfo> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class SatelliteInfo(
    val prn: Int,
    val elevation: Int,
    val azimuth: Int,
    val snr: Int,
    val usedInFix: Boolean = false
)

fun getFixQualityString(quality: Int): String {
    return when (quality) {
        1 -> "GPS Fix"
        2 -> "DGPS Fix"
        4 -> "RTK Fixed"
        5 -> "RTK Float"
        else -> "Invalid"
    }
}

sealed class DgpsStatus {
    object Idle : DgpsStatus()
    object Connecting : DgpsStatus()
    object Connected : DgpsStatus()
    data class Error(val message: String) : DgpsStatus()
}
