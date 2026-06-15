package com.rbt.survey.data.model

import com.google.gson.annotations.SerializedName

data class FormsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<FormData>?,
    @SerializedName("errors") val errors: Any?
)

data class FormData(
    @SerializedName("formId") val formId: Int,
    @SerializedName("formCode") val formCode: String,
    @SerializedName("formName") val formName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("currentVersionNo") val currentVersionNo: Int,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("createdOn") val createdOn: String?,
    @SerializedName("updatedOn") val updatedOn: String?
)

data class FileUploadResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: FileUploadData?,
    @SerializedName("errors") val errors: Any?
)

data class FileUploadData(
    @SerializedName("fileId") val fileId: Int,
    @SerializedName("formId") val formId: Int,
    @SerializedName("submissionId") val submissionId: Int,
    @SerializedName("fieldId") val fieldId: String,
    @SerializedName("originalFileName") val originalFileName: String?,
    @SerializedName("storedFileName") val storedFileName: String?,
    @SerializedName("relativePath") val relativePath: String?,
    @SerializedName("contentType") val contentType: String?,
    @SerializedName("fileSize") val fileSize: Long,
    @SerializedName("createdOn") val createdOn: String?,
    @SerializedName("uploadedBy") val uploadedBy: String?
)
