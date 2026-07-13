package com.rbt.survey.data.repository

import com.rbt.survey.data.model.*
import com.rbt.survey.data.remote.AssetApi
import okhttp3.*
import retrofit2.Response

class AssetRepository (
    private val apiService: AssetApi
) {

    suspend fun getAssetTypes(): List<AssetTypeResponse> {
        return apiService.getAssetTypes()
    }

    suspend fun createAsset(
        request: CreateAssetRequest
    ): Response<Unit> {
        return apiService.createAsset( request)
    }

    suspend fun createAssetWithImage(
        assetCode: RequestBody,
        assetName: RequestBody,
        assetCategory: RequestBody,
        assetnature: RequestBody,
        geometryType: RequestBody,
        image: MultipartBody.Part
    ): Response<Unit> {

        return apiService.createAssetWithImage(
            assetCode,
            assetName,
            assetCategory,
            assetnature,
            geometryType,
            image
        )
    }

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

    suspend fun getFormFieldMaster():
            FormFieldMasterResponse {
        return apiService.getFormFieldMaster()
    }

    suspend fun createConfiguration(
        assetTypeId: Int,
        request: SaveConfigRequest
    ): Response<Unit> {

        return apiService.createConfiguration(
            assetTypeId,
            request
        )
    }

    suspend fun saveConfiguration(
        assetTypeId: Int,
        request: SaveConfigRequest
    ): Response<Unit> {
        return apiService.saveConfiguration(
            assetTypeId,
            request
        )
    }

}