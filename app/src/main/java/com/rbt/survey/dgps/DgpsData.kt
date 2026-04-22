package com.rbt.survey.dgps

data class DgpsLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val fixQuality: Int, // 0=Invalid, 1=GPS fix, 2=DGPS fix, 4=RTK fixed, 5=RTK float
    val satellites: Int,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class DgpsStatus {
    object Idle : DgpsStatus()
    object Connecting : DgpsStatus()
    object Connected : DgpsStatus()
    data class Error(val message: String) : DgpsStatus()
}
