package com.rbt.survey.data.model

data class SubmissionResponse(
    val success: Boolean,
    val message: String?,
    val data: SubmissionData?,
    val errors: Any?
)

data class SubmissionData(
    val items: List<SubmissionItem>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

data class SubmissionItem(
    val submissionId: Int,
    val formId: Int,
    val formVersionNo: Int,
    val parentSubmissionId: String?,
    val data: SubmissionDataFields,
    val status: String,
    val createdOn: String,
    val updatedOn: String?
)

data class SubmissionDataFields(
    val GP: String?,
    val Block: String?,
    val State: String?,
    val District: String?,
    val lgd_Code: String?,
    val blockCode: String?,
    val AssetImage: Any?,
    val GPLocation: Any?
)