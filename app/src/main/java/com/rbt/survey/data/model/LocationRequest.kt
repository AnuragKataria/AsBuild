package com.rbt.survey.data.model

data class LocationRequest(
    val userId: Int,
    val userName: String,
    val email: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val deviceType: String? = null,
    val recordedAt: String
)