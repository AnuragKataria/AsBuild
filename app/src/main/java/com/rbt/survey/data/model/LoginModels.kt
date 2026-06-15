package com.rbt.survey.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("userId") val userId: Int?,
    @SerializedName("tenantId") val tenantId: Int?,
    @SerializedName("tenantCode") val tenantCode: String?,
    @SerializedName("role") val role: Role?,
    @SerializedName("token") val token: String?,
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("accessTokenExpiresAtUtc") val accessTokenExpiresAtUtc: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("refreshTokenExpiresAtUtc") val refreshTokenExpiresAtUtc: String?,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("email") val email: String?
)

data class Role(
    @SerializedName("roleId") val roleId: Int,
    @SerializedName("roleCode") val roleCode: String,
    @SerializedName("roleName") val roleName: String
)
