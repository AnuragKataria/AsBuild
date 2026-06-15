package com.rbt.survey.data.repository

import com.rbt.survey.data.model.BlockAssignmentResponse
import com.rbt.survey.data.model.BlockSummaryResponse
import com.rbt.survey.data.model.SubmissionResponse
import com.rbt.survey.data.model.SubmissionSearchRequest
import com.rbt.survey.data.remote.GeoApi
import retrofit2.Response

import com.google.gson.Gson
import com.rbt.survey.data.local.db.CachedBlockAssignment
import com.rbt.survey.data.local.db.CachedBlockAssignmentDao
import com.rbt.survey.data.local.db.CachedBlockSummary
import com.rbt.survey.data.local.db.CachedBlockSummaryDao
import com.rbt.survey.data.local.db.CachedUploadedSubmission
import com.rbt.survey.data.local.db.CachedUploadedSubmissionDao
import com.rbt.survey.data.local.db.LocationDao
import com.rbt.survey.data.local.db.LocationEntity
import com.rbt.survey.data.model.LatestLocationResponse
import com.rbt.survey.data.model.LocationRequest
import com.rbt.survey.data.model.UserLocationResponse

class GeoRepository(
    private val geoApi: GeoApi,
    private val cachedBlockAssignmentDao: CachedBlockAssignmentDao,
    private val cachedBlockSummaryDao: CachedBlockSummaryDao,
    private val cachedUploadedSubmissionDao: CachedUploadedSubmissionDao,
    private val locationDao: LocationDao
) {
    private val gson = Gson()

    suspend fun getBlockAssignments(userId: String): Response<BlockAssignmentResponse> {
        return try {
            val response = geoApi.getBlockAssignments(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                val json = gson.toJson(response.body()?.data?.assignments)
                cachedBlockAssignmentDao.insert(CachedBlockAssignment(userId, json))
            }
            response
        } catch (e: Exception) {
            val cached = cachedBlockAssignmentDao.getByUserId(userId)
            if (cached != null) {
                val assignments = gson.fromJson(cached.assignmentsJson, Array<com.rbt.survey.data.model.Assignment>::class.java).toList()
                val userIdInt = userId.toIntOrNull() ?: 0
                Response.success(BlockAssignmentResponse(true, "Loaded from cache", com.rbt.survey.data.model.BlockAssignmentData(userIdInt, assignments), null))
            } else {
                throw e
            }
        }
    }

    suspend fun getBlockSummary(userId: String, formId: Int): Response<BlockSummaryResponse> {
        return try {
            val response = geoApi.getBlockSummary(userId, formId)
            if (response.isSuccessful && response.body()?.success == true) {
                val json = gson.toJson(response.body()?.data)
                cachedBlockSummaryDao.insert(CachedBlockSummary(userId, formId, json))
            }
            response
        } catch (e: Exception) {
            val cached = cachedBlockSummaryDao.getSummary(userId, formId)
            if (cached != null) {
                val summaries = gson.fromJson(cached.summaryJson, Array<com.rbt.survey.data.model.BlockSummary>::class.java).toList()
                Response.success(BlockSummaryResponse(true, "Loaded from cache", summaries, null))
            } else {
                throw e
            }
        }
    }

    suspend fun getUploadedSubmissions(
        formId: Int,
        request: SubmissionSearchRequest
    ): Response<SubmissionResponse> {
        return try {
            val response = geoApi.getUploadedSubmissions(formId, request)
            if (response.isSuccessful && response.body()?.success == true) {
                val items = response.body()?.data?.items ?: emptyList()
                val cached = items.map {
                    CachedUploadedSubmission(formId, it.submissionId, gson.toJson(it))
                }
                cachedUploadedSubmissionDao.deleteByFormId(formId)
                cachedUploadedSubmissionDao.insert(cached)
            }
            response
        } catch (e: Exception) {
            val cached = cachedUploadedSubmissionDao.getByFormId(formId)
            if (cached.isNotEmpty()) {
                val items = cached.map {
                    gson.fromJson(it.submissionJson, com.rbt.survey.data.model.SubmissionItem::class.java)
                }
                val data = com.rbt.survey.data.model.SubmissionData(items, items.size, 1, items.size)
                Response.success(SubmissionResponse(true, "Loaded from cache", data, null))
            } else {
                throw e
            }
        }
    }

    suspend fun sendLocation(request: LocationRequest): Boolean {
        return try {
            geoApi.sendLocation(request).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllUsersLocations(): Response<UserLocationResponse> {
        return geoApi.getAllUsersLocations()
    }

    suspend fun getLatestUserLocation(
        userId: Int
    ): Response<LatestLocationResponse> {
        return geoApi.getLatestUserLocation(userId)
    }

    suspend fun getUserLocationHistory(
        userId: Int,
        from: String,
        to: String
    ): Response<UserLocationResponse> {
        return geoApi.getUserLocationHistory(
            userId = userId,
            from = from,
            to = to
        )
    }

    // 🔴 Save offline
    suspend fun saveLocationOffline(location: LocationEntity) {
        locationDao.insert(location)
    }

    // 📦 Get all pending
    suspend fun getPendingLocations(): List<LocationEntity> {
        return locationDao.getAllLocations()
    }

    // 🗑 Delete after attempt
    suspend fun deleteLocation(location: LocationEntity) {
        locationDao.delete(location)
    }

}

fun LocationEntity.toRequest(includeRecordedAt: Boolean = true): LocationRequest {
    return LocationRequest(
        userId = userId,
        userName = userName,
        email = email,
        lat = lat,
        lng = lng,
        accuracy = accuracy,
        altitude = altitude,
        speed = speed,
        heading = heading,
        deviceType = deviceType,
        recordedAt = if (includeRecordedAt) recordedAt else null
    )
}

