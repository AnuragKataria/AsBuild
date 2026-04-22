package com.rbt.survey.data.remote

import com.rbt.survey.data.model.FormDetailResponse
import com.rbt.survey.data.model.FormsResponse
import com.rbt.survey.data.model.LoginRequest
import com.rbt.survey.data.model.LoginResponse
import com.rbt.survey.data.model.RefreshTokenRequest
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("Auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("Auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @retrofit2.http.Multipart
    @retrofit2.http.POST("forms/{formId}/files")
    suspend fun uploadFile(
        @retrofit2.http.Path("formId") formId: Int,
        @retrofit2.http.Part("FieldId") fieldIdPart: okhttp3.RequestBody,
        @retrofit2.http.Part("UploadedBy") uploadedByPart: okhttp3.RequestBody,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): Response<com.rbt.survey.data.model.FileUploadResponse>

    @retrofit2.http.GET("forms")
    suspend fun getForms(): Response<FormsResponse>

    @retrofit2.http.GET("forms/{formId}")
    suspend fun getFormDetail(
        @retrofit2.http.Path("formId") formId: Int,
        @retrofit2.http.Query("blockCode") blockCode: String?
    ): Response<FormDetailResponse>
    
//    @POST("forms/{formId}/submissions")
//    suspend fun submitForm(
//        @retrofit2.http.Path("formId") formId: Int,
//        @Body request: Map<String, Any?>
//    ): Response<okhttp3.ResponseBody>
    @POST("forms/{formId}/submissions")
    suspend fun submitForm(
        @retrofit2.http.Path("formId") formId: Int,
        @Body body: RequestBody
    ): Response<okhttp3.ResponseBody>
}
