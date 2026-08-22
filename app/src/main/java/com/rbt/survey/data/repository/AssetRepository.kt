package com.rbt.survey.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.rbt.survey.data.model.*
import com.rbt.survey.data.remote.AssetApi
import com.rbt.survey.ui.incidentManagement.getFileName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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

    suspend fun getCretedAssetDetail(
        assetId: Int
    ): CreatedAssetDetailResponse {
        return apiService.getCretedAssetDetail(assetId)
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

    suspend fun getcustomers(): List<CustomerResponse> {
        return apiService.getcustomers()
    }


    suspend fun getFmsUtilization(
        assetId: Int
    ): List<FmsPortUtilizationResponse> {
        return apiService.getFmsUtilization(assetId)
    }

    suspend fun getFiberCoreStructure(
        assetId: Int
    ): FiberStructureResponse {
        return apiService.getFiberCoreStructure(assetId)
    }

    suspend fun getFiberCoreUtilization(
        assetId: Int
    ): List<FiberCoreUtilizationResponse> {
        return apiService.getFiberCoreUtilization(assetId)
    }

    suspend fun createTermination(
        request: JSONObject
    ): Response<Unit> {

        val body = request.toString()
            .toRequestBody("application/json".toMediaType())

        return apiService.createTermination(body)
    }

    suspend fun createSplice(
        request: JSONObject
    ): Response<Unit> {

        val body = request.toString()
            .toRequestBody("application/json".toMediaType())

        return apiService.createSplice(body)
    }

    suspend fun updatePortHealthStatus(
        request: JSONObject
    ): Response<Unit> {
        val body = request.toString()
            .toRequestBody("application/json".toMediaType())
        return apiService.updatePortHealthStatus(body)
    }

    suspend fun getCustomerMappings(
        assetId: Int
    ): List<CustomerMappingResponse> {

        return apiService.getCustomerMappings(assetId)
    }

    suspend fun updateCustomerPort(
        request: JSONObject
    ): Response<ResponseBody> {
        val body = request.toString()
            .toRequestBody("application/json".toMediaType())
        return apiService.updateCustomerPort(body)
    }

    suspend fun exportSpliceClosureDiagramPdf(
        assetId: Int
    ) = apiService.exportSpliceClosureDiagramPdf(assetId)


    //----------------------Incident Management--------------------

    suspend fun getIncidents(
        projectId: Long? = null,
        status: String? = null,
        priority: String? = null,
        category: String? = null,
        assetId: Long? = null,
        assigneeId: Long? = null,
        searchQuery: String? = null,
        pageNumber: Int = 1,
        pageSize: Int = 100
    ): IncidentResponse {

        return apiService.getIncidents(
            projectId = projectId,
            status = status,
            priority = priority,
            category = category,
            assetId = assetId,
            assigneeId = assigneeId,
            searchQuery = searchQuery,
            pageNumber = pageNumber,
            pageSize = pageSize
        )
    }

    suspend fun logIncident(
        context: Context,
        projectId: Long,
        assetId: Long?,
        title: String,
        category: String,
        priority: String,
        description: String,
        latitude: Double,
        longitude: Double,
        attachments: List<Uri>?
    ): Response<ResponseBody> {

        fun String.toTextRequestBody(): RequestBody {
            return this.toRequestBody("text/plain".toMediaType())
        }

        val projectIdBody = projectId.toString().toTextRequestBody()
        val assetIdBody = assetId?.toString()?.toTextRequestBody()
        val titleBody = title.toTextRequestBody()
        val descriptionBody = description.toTextRequestBody()
        val categoryBody = category.toTextRequestBody()
        val priorityBody = priority.toTextRequestBody()
        val latitudeBody = latitude.toString().toTextRequestBody()
        val longitudeBody = longitude.toString().toTextRequestBody()

        val attachmentParts =
            attachments?.mapNotNull { uri ->
                try {

                    val contentResolver = context.contentResolver

                    val fileName = getFileName(context, uri)

                    val mimeType =
                        contentResolver.getType(uri)
                            ?: "application/octet-stream"

                    val bytes =
                        contentResolver
                            .openInputStream(uri)
                            ?.use { inputStream ->
                                inputStream.readBytes()
                            }
                            ?: return@mapNotNull null

                    val requestBody =
                        bytes.toRequestBody(
                            mimeType.toMediaType()
                        )

                    MultipartBody.Part.createFormData(
                        "attachments",
                        fileName,
                        requestBody
                    )

                } catch (e: Exception) {

                    Log.e(
                        "REPORT_INCIDENT",
                        "Failed to process attachment: $uri",
                        e
                    )

                    null
                }

            } ?: emptyList()

        return apiService.logIncident(
            projectId = projectIdBody,
            assetId = assetIdBody,
            title = titleBody,
            description = descriptionBody,
            category = categoryBody,
            priority = priorityBody,
            latitude = latitudeBody,
            longitude = longitudeBody,
            attachments = attachmentParts
        )
    }

}