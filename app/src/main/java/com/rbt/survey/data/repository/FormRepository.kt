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

class FormRepository(
    private val authApi: AuthApi,
    private val draftDao: FormDraftDao,
    private val offlineSubmissionDao: OfflineSubmissionDao
) {
    suspend fun uploadFile(
        context: android.content.Context,
        formId: Int,
        fieldId: String,
        uploadedBy: String,
        uriString: String
    ): Response<FileUploadResponse> {
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

        return authApi.uploadFile(formId, fieldIdBody, uploadedByBody, filePart)
    }

    suspend fun getForms(): Response<FormsResponse> {
        return authApi.getForms()
    }

    suspend fun getFormDetail(formId: Int, blockCode: String?): Response<FormDetailResponse> {
        return authApi.getFormDetail(formId, blockCode)
    }

    suspend fun saveDraft(formId: Int, fieldId: String, value: String) {
        draftDao.saveDraft(FormDraft(formId, fieldId, value))
    }

    suspend fun getDrafts(formId: Int): List<FormDraft> {
        return draftDao.getDraftsForForm(formId)
    }

    suspend fun clearDrafts(formId: Int) {
        draftDao.clearDraftsForForm(formId)
    }

//    suspend fun submitForm(formId: Int, request: Map<String, Any?>): Response<okhttp3.ResponseBody> {
//        return authApi.submitForm(formId, request)
//    }

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
