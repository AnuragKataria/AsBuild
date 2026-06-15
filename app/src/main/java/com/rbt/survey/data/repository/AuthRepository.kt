package com.rbt.survey.data.repository

import com.rbt.survey.data.model.LoginRequest
import com.rbt.survey.data.model.LoginResponse
import com.rbt.survey.data.remote.AuthApi
import retrofit2.Response

class AuthRepository(private val authApi: AuthApi) {
    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return authApi.login(request)
    }
}
