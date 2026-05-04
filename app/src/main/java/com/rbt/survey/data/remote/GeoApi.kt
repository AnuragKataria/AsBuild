package com.rbt.survey.data.remote

import com.rbt.survey.data.model.BlockAssignmentResponse
import com.rbt.survey.data.model.BlockSummaryResponse
import com.rbt.survey.data.model.LocationRequest
import com.rbt.survey.data.model.SubmissionResponse
import com.rbt.survey.data.model.SubmissionSearchRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeoApi {
    @GET("geo-hierarchy/users/{userId}/block-assignments")
    suspend fun getBlockAssignments(
        @Path("userId") userId: String
    ): Response<BlockAssignmentResponse>

    @GET("geo-hierarchy/users/{userId}/block-summary")
    suspend fun getBlockSummary(
        @Path("userId") userId: String,
        @Query("formId") formId: Int
    ): Response<BlockSummaryResponse>

    @POST("forms/{formId}/submissions/search")
    suspend fun getUploadedSubmissions(
        @Path("formId") formId: Int,
        @Body request: SubmissionSearchRequest
    ): Response<SubmissionResponse>

    @POST("v1/locations")
    suspend fun sendLocation(
        @Body request: LocationRequest
    ): Response<Unit>
}

