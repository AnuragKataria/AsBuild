package com.rbt.survey.data.repository

import com.rbt.survey.data.local.db.FormDraft
import com.rbt.survey.data.local.db.FormDraftDao
import com.rbt.survey.data.model.FormDetailResponse
import com.rbt.survey.data.model.FileUploadResponse
import com.rbt.survey.data.model.FormsResponse
import com.rbt.survey.data.remote.AuthApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

import com.rbt.survey.data.local.db.OfflineSubmission
import com.rbt.survey.data.local.db.OfflineSubmissionDao

import com.rbt.survey.data.local.db.CachedForm
import com.rbt.survey.data.local.db.CachedFormDao
import com.rbt.survey.data.local.db.CachedFormDetail
import com.rbt.survey.data.local.db.CachedFormDetailDao
import com.rbt.survey.data.local.db.PendingFileUpload
import com.rbt.survey.data.local.db.PendingFileUploadDao
import com.google.gson.Gson

class FormRepository(
    private val authApi: AuthApi,
    private val draftDao: FormDraftDao,
    private val offlineSubmissionDao: OfflineSubmissionDao,
    private val cachedFormDao: CachedFormDao,
    private val cachedFormDetailDao: CachedFormDetailDao,
    private val pendingFileUploadDao: PendingFileUploadDao
) {
    private val gson = Gson()

    suspend fun uploadFile(
        context: android.content.Context,
        formId: Int,
        fieldId: String,
        uploadedBy: String,
        uriString: String
    ): Response<FileUploadResponse> {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val contentResolver = context.contentResolver
            
            // Get file name and content type
            var fileName = "file.jpg"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            
            val inputStream = contentResolver.openInputStream(uri) ?: throw java.io.IOException("Could not open input stream")
            val mediaType = (contentResolver.getType(uri) ?: "image/jpeg").toMediaTypeOrNull()
            val requestBody = inputStream.readBytes().toRequestBody(mediaType)
            
            val filePart = MultipartBody.Part.createFormData("File", fileName, requestBody)
            
            val plainTextMediaType = "text/plain".toMediaTypeOrNull()
            val fieldIdBody = fieldId.toRequestBody(plainTextMediaType)
            val uploadedByBody = uploadedBy.toRequestBody(plainTextMediaType)

            val response = authApi.uploadFile(formId, fieldIdBody, uploadedByBody, filePart)
            
            if (!response.isSuccessful) {
                // If API call fails, queue it for later
                savePendingUpload(formId, fieldId, uploadedBy, uriString)
            }
            
            response
        } catch (e: Exception) {
            // If network or other error occurs, queue it for later
            savePendingUpload(formId, fieldId, uploadedBy, uriString)
            throw e
        }
    }

    private suspend fun savePendingUpload(formId: Int, fieldId: String, uploadedBy: String, uriString: String) {
        pendingFileUploadDao.insert(
            PendingFileUpload(
                formId = formId,
                fieldId = fieldId,
                uploadedBy = uploadedBy,
                uriString = uriString
            )
        )
    }

    suspend fun getForms(): Response<FormsResponse> {
        return try {
            val response = authApi.getForms()
            if (response.isSuccessful && response.body()?.success == true) {
                val forms = response.body()?.data?.map {
                    CachedForm(it.formId, it.formName, it.formCode, it.description, it.isActive)
                }
                if (forms != null) {
                    cachedFormDao.insertForms(forms)
                }
            }
            response
        } catch (e: Exception) {
            // If offline, return cached forms
            val cachedForms = cachedFormDao.getAllForms().map {
                com.rbt.survey.data.model.FormData(it.formId, it.formCode, it.formName, it.description, 1, it.isActive, null, null)
            }
            Response.success(FormsResponse(true, "Loaded from cache", cachedForms, null))
        }
    }

    suspend fun getFormDetail(formId: Int, blockCode: String?): Response<FormDetailResponse> {
        val bCode = blockCode ?: ""
        return try {
            val response = authApi.getFormDetail(formId, blockCode)
            if (response.isSuccessful && response.body()?.success == true) {
                val detailJson = gson.toJson(response.body())
                cachedFormDetailDao.insertFormDetail(CachedFormDetail(formId, bCode, detailJson))
            }
            response
        } catch (e: Exception) {
            // If offline, return cached form detail
            val cachedDetail = cachedFormDetailDao.getFormDetail(formId, bCode)
            if (cachedDetail != null) {
                val detailData = gson.fromJson(cachedDetail.detailJson, FormDetailResponse::class.java)
                Response.success(detailData)
            } else {
                throw e
            }
        }
    }

    suspend fun getDownloadedBlockCodes(formId: Int): List<String> {
        return cachedFormDetailDao.getDownloadedBlockCodes(formId)
    }

    suspend fun saveDraft(formId: Int, fieldId: String, gp: String, value: String) {
        draftDao.saveDraft(FormDraft(formId, fieldId,gp, value))
    }

    suspend fun getDrafts(formId: Int, gp: String): List<FormDraft> {
        return draftDao.getDraftsForForm(formId,gp)
    }

    suspend fun clearDrafts(formId: Int, gp: String) {
        draftDao.clearDraftsForForm(formId,gp)
    }

    suspend fun saveOfflineSubmission(submission: OfflineSubmission) {
        offlineSubmissionDao.insert(submission)
    }

    suspend fun getOfflineSubmissions(formId: Int): List<OfflineSubmission> {
        return offlineSubmissionDao.getSubmissionsForForm(formId)
    }

    suspend fun getOfflineSubmissionById(id: Int): OfflineSubmission? {
        return offlineSubmissionDao.getSubmissionById(id)
    }

    suspend fun deleteOfflineSubmission(id: Int) {
        offlineSubmissionDao.deleteSubmission(id)
    }

    suspend fun submitForm(formId: Int, body: RequestBody): Response<okhttp3.ResponseBody> {
        return authApi.submitForm(formId, body)
    }
}
