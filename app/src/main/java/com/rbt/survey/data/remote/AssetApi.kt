package com.rbt.survey.data.remote

import com.google.gson.JsonObject
import com.rbt.survey.data.model.*
import okhttp3.*
import retrofit2.http.*
import retrofit2.Response

interface AssetApi {

    //-------------------AssetManagement----------------------------

    @GET("ColorMaster/assets")
    suspend fun getAssetTypes(): List<AssetTypeResponse>

//    @POST("AssetType")
//    suspend fun createAsset(
//        @Body request: CreateAssetRequest
//    ): Response<Unit>
//
//    @Multipart
//    @POST("AssetType/with-image")
//    suspend fun createAssetWithImage(
//        @Part("AssetCode") assetCode: RequestBody,
//
//        @Part("AssetName") assetName: RequestBody,
//
//        @Part("AssetCategory") assetCategory: RequestBody,
//
//        @Part("Assetnature") assetnature: RequestBody,
//
//        @Part("GeometryType") geometryType: RequestBody,
//
//        @Part image: MultipartBody.Part
//    ): Response<Unit>

    @GET("AssetType/{assetTypeId}")
    suspend fun getAssetDetail(
        @Path("assetTypeId") assetTypeId: Int
    ): AssetDetailResponse

    @GET("assets/{assetId}/dynamic-fields/details")
    suspend fun getCretedAssetDetail(
        @Path("assetId") assetTypeId: Int
    ): CreatedAssetDetailResponse


    @GET("asset-types/{assetTypeId}/dynamic-fields/config")
    suspend fun getAssetConfig(
        @Path("assetTypeId") assetTypeId: Int
    ): Response<AssetConfigResponse>


//    @GET("form-field-masters")
//    suspend fun getFormFieldMaster():
//            FormFieldMasterResponse
//
//    @POST("asset-types/{assetTypeId}/dynamic-fields/config")
//    suspend fun createConfiguration(
//        @Path("assetTypeId") assetTypeId: Int,
//        @Body request: SaveConfigRequest
//    ): Response<Unit>
//
//    @POST("asset-types/{assetTypeId}/dynamic-fields/versions")
//    suspend fun saveConfiguration(
//        @Path("assetTypeId") assetTypeId: Int,
//        @Body request: SaveConfigRequest
//    ): Response<Unit>

    //-------------------AssetAddProject----------------------------

    @GET("Project")
    suspend fun getProjects(): List<ProjectResponse>

//    @POST("asset-dynamic-fields/dropdowns/resolve")
//    suspend fun getStates(
//        @Body request: JsonObject
//    ): DropdownResponse
//
//    @POST("asset-dynamic-fields/dropdowns/resolve-multiple")
//    suspend fun getDistricts(
//        @Body request: JsonObject
//    ): DropdownResponse
//
//    @POST("asset-dynamic-fields/dropdowns/resolve-multiple")
//    suspend fun getBlocks(
//        @Body request: JsonObject
//    ): DropdownResponse

    @POST("assets")
    suspend fun createAddAsset(
        @Body request: CreateAddAssetRequest
    ): Int

    @PUT("assets/{assetId}/dynamic-fields")
    suspend fun updateDynamicFields(
        @Path("assetId") assetId: Int,
        @Body request: UpdateDynamicFieldsRequest
    ): Response<Unit>

    @GET("dynamic-connectivity/rules")
    suspend fun getConnectivityRules(): List<ConnectivityRuleResponse>

    @POST("projects/{projectId}/asset-dynamic-fields/created-assets/search-slim")
    suspend fun getCreatedAssets(
        @Path("projectId") projectId: Int,
        @Body body: JsonObject
    ): CreatedAssetsResponse

    @GET("/api/route-trace/{assetId}/report.pdf")
    @Streaming
    suspend fun exportPdf(
        @Path("assetId") assetId: Int
    ): Response<ResponseBody>

    @GET("customers")
    suspend fun getcustomers(): List<CustomerResponse>

    @GET("assets/{assetId}/fms-ports/utilization")
    suspend fun getFmsUtilization(
        @Path("assetId") assetId: Int
    ): List<FmsPortUtilizationResponse>

    @GET("assets/{assetId}/fiber-structure")
    suspend fun getFiberCoreStructure(
        @Path("assetId") assetId: Int
    ): FiberStructureResponse


    @GET("assets/{assetId}/fiber-structure/cores/utilization")
    suspend fun getFiberCoreUtilization(
        @Path("assetId") assetId: Int
    ): List<FiberCoreUtilizationResponse>

    @POST("fiber-terminations")
    suspend fun createTermination(
        @Body request: RequestBody
    ): Response<Unit>

    @POST("fiber-terminations/ports/health-status")
    suspend fun updatePortHealthStatus(
        @Body request: RequestBody
    ): Response<Unit>
}