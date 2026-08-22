package com.rbt.survey.data.model

data class IncidentResponse(
    val items: List<IncidentItem>,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int
)

data class IncidentItem(
    val incidentId: Int,
    val incidentCode: String,
    val projectId: Int,
    val projectName: String,
    val assetId: Int,
    val assetCodeSnapshot: String?,
    val assetNameSnapshot: String?,
    val assetCodeCurrent: String?,
    val title: String,
    val category: String?,
    val status: String,
    val priority: String,
    val reportedByUserName: String?,
    val assignedToUserName: String?,
    val createdOn: String,
    val updatedOn: String?,
    val resolvedOn: String?,
    val hoursSpent: Double?,
    val slaResolutionHours: Double?,
    val targetResolutionTime: String?,
    val isSlaBreached: Boolean
)

data class IncidentFilter(
    val projectId: Long? = null,
    val status: String? = null,
    val priority: String? = null,
    val category: String? = null,
    val assetId: Long? = null,
    val assigneeId: Long? = null,
    val searchQuery: String? = null
)