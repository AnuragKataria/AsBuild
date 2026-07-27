package com.rbt.survey.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rbt.survey.data.model.*
import com.rbt.survey.data.remote.AssetApi
import okhttp3.*
import retrofit2.Response

class AssetRepository (
    private val apiService: AssetApi
) {

    //----------------------AssetManagement--------------------

    suspend fun getAssetTypes(): List<AssetTypeResponse> {
        return apiService.getAssetTypes()
    }

//    suspend fun createAsset(
//        request: CreateAssetRequest
//    ): Response<Unit> {
//        return apiService.createAsset( request)
//    }
//
//    suspend fun createAssetWithImage(
//        assetCode: RequestBody,
//        assetName: RequestBody,
//        assetCategory: RequestBody,
//        assetnature: RequestBody,
//        geometryType: RequestBody,
//        image: MultipartBody.Part
//    ): Response<Unit> {
//
//        return apiService.createAssetWithImage(
//            assetCode,
//            assetName,
//            assetCategory,
//            assetnature,
//            geometryType,
//            image
//        )
//    }

    suspend fun getAssetDetail(
        assetTypeId: Int
    ): AssetDetailResponse {
        return apiService.getAssetDetail(assetTypeId)
    }

    suspend fun getAssetConfig(
        assetTypeId: Int
    ): Response<AssetConfigResponse> {
        return apiService.getAssetConfig(assetTypeId)
    }

//    suspend fun getFormFieldMaster():
//            FormFieldMasterResponse {
//        return apiService.getFormFieldMaster()
//    }
//
//    suspend fun createConfiguration(
//        assetTypeId: Int,
//        request: SaveConfigRequest
//    ): Response<Unit> {
//
//        return apiService.createConfiguration(
//            assetTypeId,
//            request
//        )
//    }
//
//    suspend fun saveConfiguration(
//        assetTypeId: Int,
//        request: SaveConfigRequest
//    ): Response<Unit> {
//        return apiService.saveConfiguration(
//            assetTypeId,
//            request
//        )
//    }

    //----------------------AssetAddProject--------------------

    suspend fun getProjects(): List<ProjectResponse> {
        return apiService.getProjects()
    }

//    suspend fun getStates(projectId: Int): DropdownResponse {
//
//        val request = JsonObject().apply {
//            addProperty("projectId", projectId)
//            addProperty("level", "state")
//        }
//        return apiService.getStates(request)
//    }
//
//    suspend fun getDistricts(projectId: Int, stateCodes: List<String>): DropdownResponse {
//
//        val request = JsonObject().apply {
//            add("projectIds", Gson().toJsonTree(listOf(projectId)))
//            addProperty("level", "district")
//            add("stateCodes",Gson().toJsonTree(stateCodes))
//            add("districtCodes", Gson().toJsonTree(emptyList<String>()))
//        }
//        return apiService.getDistricts(request)
//    }
//
//    suspend fun getBlocks(projectId: Int, stateCodes: List<String>, districtCodes: List<String>): DropdownResponse {
//
//        val request = JsonObject().apply {
//            add("projectIds", Gson().toJsonTree(listOf(projectId)))
//            addProperty("level", "block")
//            add("stateCodes", Gson().toJsonTree(stateCodes))
//            add("districtCodes", Gson().toJsonTree(districtCodes))
//        }
//        return apiService.getBlocks(request)
//    }


    suspend fun createAddAsset(
        request: CreateAddAssetRequest
    ): Int {
        return apiService.createAddAsset(request)
    }

    suspend fun updateDynamicFields(
        assetId: Int,
        request: UpdateDynamicFieldsRequest
    ) {
        apiService.updateDynamicFields(assetId, request)
    }

    suspend fun getConnectivityRules(): Result<List<ConnectivityRuleResponse>> {
        return try {
            Result.success(apiService.getConnectivityRules())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCreatedAssets(
        projectId: Int
    ): CreatedAssetsResponse {
        val body = JsonObject().apply {
            add("stateCodes", JsonArray())
            add("districtCodes", JsonArray())
            add("blockCodes", JsonArray())
        }
        return apiService.getCreatedAssets(
            projectId,
            body
        )
    }

    suspend fun exportPdf(
        assetId: Int
    ) = apiService.exportPdf(assetId)
}