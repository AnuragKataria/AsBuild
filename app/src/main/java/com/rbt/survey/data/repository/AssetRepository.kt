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
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File

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

    suspend fun getIncidentDetails(
        incidentId: Int
    ): IncidentDetailsResponse {

        val response = apiService.getIncidentDetails(incidentId)

        if (response.isSuccessful) {
            return response.body()
                ?: throw Exception("Empty response")
        }

        throw Exception(
            response.errorBody()?.string()
                ?: "Failed to load incident details"
        )
    }

    suspend fun getIncidentImpactDetails(
        incidentId: Int
    ): List<IncidentImpactAssetDetails> {

        val response = apiService.getIncidentImpactDetails(incidentId)

        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        }

        throw Exception(
            response.errorBody()?.string()
                ?: "Failed to load impact data"
        )
    }

    suspend fun downloadIncidentReport(
        incidentId: Int
    ): Response<ResponseBody> {
        return apiService.downloadIncidentReport(incidentId)
    }

    suspend fun addIncidentComment(
        incidentId: Int,
        request: RequestBody
    ): Boolean {

        val response = apiService.addIncidentComment(
            incidentId,
            request
        )

        return response.isSuccessful
    }

    suspend fun uploadOTDRTrace(
        file: File,
        fromStation: String,
        toStation: String,
        uploadedBy: String,
        deviceType: String
    ): Result<Boolean> {

        return try {

            val requestFile =
                file.asRequestBody("multipart/form-data".toMediaType())

            val filePart =
                MultipartBody.Part.createFormData(
                    "File",
                    file.name,
                    requestFile
                )

            val response = apiService.uploadOTDRTrace(
                file = filePart,
                fromStationName = fromStation.toRequestBody("text/plain".toMediaType()),
                toStationName = toStation.toRequestBody("text/plain".toMediaType()),
                reportUploadedBy = uploadedBy.toRequestBody("text/plain".toMediaType()),
                otdrDeviceType = deviceType.toRequestBody("text/plain".toMediaType())
            )

            if (response.isSuccessful) {

                Result.success(true)

            } else {

                Result.failure(
                    Exception(
                        "Upload failed: ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOTDRReports(): Result<JsonObject> {

        return try {
            val response = apiService.getOTDRReports()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception(
                        "Failed to load OTDR reports: ${response.code()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getOTDRSiteNames(): Result<List<String>> {

        return try {

            val response = apiService.getOTDRSiteNames()

            if (response.isSuccessful) {

                val body = response.body()

                if (body != null &&
                    body.get("success")?.asBoolean == true
                ) {

                    val data = body.getAsJsonArray("data")

                    val siteNames = data.map {
                        it.asString
                    }

                    Result.success(siteNames)

                } else {

                    Result.failure(
                        Exception(
                            body?.get("message")?.asString
                                ?: "Failed to load site names"
                        )
                    )
                }

            } else {

                Result.failure(
                    Exception(
                        "Failed to load site names: ${response.code()}"
                    )
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

}