package com.rbt.survey.data.remote

import com.rbt.survey.data.model.OptionSegmentResponse
import java.util.List
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TnctApi {

    @GET("GetOptionSegmentLayerByBlockCode")
    suspend fun getOptionSegmentLayerByBlockCode(
        @Query("BlockCode") blockCode: String
    ): Response<List<OptionSegmentResponse>>
}