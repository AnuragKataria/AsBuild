package com.rbt.survey.data.remote

import com.rbt.survey.data.model.*
import okhttp3.*
import retrofit2.http.*
import retrofit2.Response

interface AssetApi {

    @GET("ColorMaster/assets")
    suspend fun getAssetTypes(): List<AssetTypeResponse>

    @POST("AssetType")
    suspend fun createAsset(
        @Body request: CreateAssetRequest
    ): Response<Unit>

    @Multipart
    @POST("AssetType/with-image")
    suspend fun createAssetWithImage(
        @Part("AssetCode") assetCode: RequestBody,

        @Part("AssetName") assetName: RequestBody,

        @Part("AssetCategory") assetCategory: RequestBody,

        @Part("Assetnature") assetnature: RequestBody,

        @Part("GeometryType") geometryType: RequestBody,

        @Part image: MultipartBody.Part
    ): Response<Unit>

    @GET("AssetType/{assetTypeId}")
    suspend fun getAssetDetail(
        @Path("assetTypeId") assetTypeId: Int
    ): AssetDetailResponse


    @GET("asset-types/{assetTypeId}/dynamic-fields/config")
    suspend fun getAssetConfig(
        @Path("assetTypeId") assetTypeId: Int
    ): Response<AssetConfigResponse>


    @GET("form-field-masters")
    suspend fun getFormFieldMaster():
            FormFieldMasterResponse

    @POST("asset-types/{assetTypeId}/dynamic-fields/config")
    suspend fun createConfiguration(
        @Path("assetTypeId") assetTypeId: Int,
        @Body request: SaveConfigRequest
    ): Response<Unit>

    @POST("asset-types/{assetTypeId}/dynamic-fields/versions")
    suspend fun saveConfiguration(
        @Path("assetTypeId") assetTypeId: Int,
        @Body request: SaveConfigRequest
    ): Response<Unit>


}