package com.rbt.survey.data.repository

import com.rbt.survey.data.model.BlockAssignmentResponse
import com.rbt.survey.data.model.BlockSummaryResponse
import com.rbt.survey.data.model.SubmissionResponse
import com.rbt.survey.data.model.SubmissionSearchRequest
import com.rbt.survey.data.remote.GeoApi
import retrofit2.Response

class GeoRepository(private val geoApi: GeoApi) {
    suspend fun getBlockAssignments(userId: String): Response<BlockAssignmentResponse> {
        return geoApi.getBlockAssignments(userId)
    }

    suspend fun getBlockSummary(userId: String, formId: Int): Response<BlockSummaryResponse> {
        return geoApi.getBlockSummary(userId, formId)
    }

    suspend fun getUploadedSubmissions(
        formId: Int,
        request: SubmissionSearchRequest
    ): Response<SubmissionResponse> {
        return geoApi.getUploadedSubmissions(formId, request)
    }

}

