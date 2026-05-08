package com.rbt.survey.data.model

data class UserLocationResponse(
    val items: List<UserLocationItem> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
    val status: String? = null,
    val from: String? = null,
    val to: String? = null
)

data class UserLocationItem(
    val status: String? = null,
    val id: String? = null,
    val tenantId: Int? = null,
    val userId: Int? = null,
    val userName: String? = null,
    val email: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val deviceType: String? = null,
    val ipAddress: String? = null,
    val recordedAt: String? = null,
    val createdAt: String? = null
)

data class LatestLocationResponse(
    val id: String? = null,
    val tenantId: Int? = null,
    val userId: Int? = null,
    val userName: String? = null,
    val email: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val deviceType: String? = null,
    val ipAddress: String? = null,
    val recordedAt: String? = null,
    val createdAt: String? = null
)