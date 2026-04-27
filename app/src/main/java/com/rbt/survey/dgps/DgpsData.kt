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
        0 -> "Initialized"
        1 -> "Single"
        2 -> "Code Differential"
        3 -> "Invalid PPS"
        4 -> "Fixed"
        5 -> "Float"
        6 -> "Estimating"
        7 -> "Manual Fixed Value"
        8 -> "Simulated Mode"
        9 -> "WAAS Differential"
        else -> "Unknown"
    }
}

sealed class DgpsStatus {
    object Idle : DgpsStatus()
    object Connecting : DgpsStatus()
    object Connected : DgpsStatus()
    data class Error(val message: String) : DgpsStatus()
}
