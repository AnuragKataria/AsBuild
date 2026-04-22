package com.rbt.survey.data.remote

import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.data.model.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val userPreferences: UserPreferences,
    private val refreshApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // If we've already tried to refresh and failed 3 times, stop
        if (response.responseCount >= 3) {
            return null
        }

        val refreshToken = runBlocking {
            userPreferences.refreshToken.first()
        }

        if (refreshToken.isNullOrEmpty()) return null

        // Synchronously call the refresh token API
        val refreshResponse = runBlocking {
            try {
                refreshApi.refreshToken(RefreshTokenRequest(refreshToken))
            } catch (e: Exception) {
                null
            }
        }

        if (refreshResponse != null && refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
            val newData = refreshResponse.body()!!
            val currentUserId = runBlocking { userPreferences.userId.first() ?: "" }
            val currentName = runBlocking { userPreferences.userName.first() ?: "" }
            
            val newToken = newData.token ?: newData.accessToken ?: ""
            val newRefreshToken = newData.refreshToken ?: ""

            runBlocking {
                userPreferences.saveAuthData(
                    token = newToken,
                    refreshToken = newRefreshToken,
                    name = newData.fullName ?: currentName,
                    email = newData.email ?: "",
                    userId = newData.userId?.toString() ?: currentUserId
                )
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        } else {
            // Refresh failed, possibly log out user or clear preferences
            runBlocking {
                userPreferences.clearAuthData()
            }
            return null
        }
    }

    private val Response.responseCount: Int
        get() {
            var result = 1
            var prior = priorResponse
            while (prior != null) {
                result++
                prior = prior.priorResponse
            }
            return result
        }
}
