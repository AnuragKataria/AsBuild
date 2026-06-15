package com.rbt.survey.data.model

data class SubmissionSearchRequest(
    val parentSubmissionId: String? = null,
    val page: Int = 1,
    val pageSize: Int = 10,
    val filters: List<Any> = emptyList()
)