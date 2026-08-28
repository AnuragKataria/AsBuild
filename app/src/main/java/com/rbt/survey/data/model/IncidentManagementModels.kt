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


data class IncidentDetailsResponse(
    val incident: IncidentDetail,
    val comments: List<IncidentComment>,
    val attachments: List<IncidentAttachment>,
    val utilizedAssets: List<UtilizedAsset>
)

data class IncidentDetail(
    val incidentId: Int,
    val incidentCode: String,
    val tenantId: Int,
    val projectId: Int,
    val projectName: String,
    val projectCode: String,

    val assetId: Int?,
    val assetCodeSnapshot: String?,
    val assetNameSnapshot: String?,
    val assetCodeCurrent: String?,
    val assetNameCurrent: String?,

    val affectedCores: List<AffectedCore>,
    val affectedPorts: List<AffectedPort>,

    val title: String?,
    val description: String?,
    val category: String?,
    val status: String?,
    val priority: String?,

    val reportedByUserId: Int?,
    val reportedByUserName: String?,

    val assignedToUserId: Int?,
    val assignedToUserName: String?,

    val latitude: Double?,
    val longitude: Double?,
    val geomWkt: String?,

    val resolutionNotes: String?,
    val resolutionCategory: String?,

    val resolvedByUserId: Int?,
    val resolvedByUserName: String?,

    val closedByUserId: Int?,
    val closedByUserName: String?,

    val version: Int,
    val createdOn: String?,
    val updatedOn: String?,
    val resolvedOn: String?,
    val closedOn: String?,

    val hoursSpent: Double?,
    val slaResolutionHours: Double?,
    val targetResolutionTime: String?,
    val isSlaBreached: Boolean
)

data class AffectedCore(
    val fiberCoreId: Int,
    val coreNo: Int,
    val tubeNo: Int,
    val assetId: Int,
    val assetCode: String
)

data class AffectedPort(
    val portId: Int,
    val portNo: Int,
    val portType: String,
    val assetId: Int,
    val assetCode: String
)

data class IncidentComment(
    val commentId: Int,
    val incidentId: Int,
    val userId: Int,
    val userName: String,
    val commentText: String,
    val statusChangedTo: String?,
    val createdOn: String
)

data class IncidentAttachment(
    val attachmentId: Int,
    val incidentId: Int,
    val fileName: String,
    val filePath: String,
    val uploadedByUserId: Int,
    val uploadedByUserName: String,
    val createdOn: String
)

data class UtilizedAsset(
    val assetId: Int? = null,
    val assetCode: String? = null,
    val assetName: String? = null
)



data class IncidentImpactAssetDetails(
    val assetId: Int,
    val assetCode: String?,
    val assetName: String?,
    val assetTypeCode: String?,

    val totalCapacity: Double?,
    val affectedCapacity: Double?,
    val impactPercentage: Double?,

    val severityLevel: String?,
    val mapColor: String?,
    val mapMarkerIcon: String?,

    val depth: Int?,

    val propagationPath: List<Int>?,

    val latitude: Double?,
    val longitude: Double?,
    val geomWkt: String?,

    val affectedElementIds: List<Int>?,
    val affectedElementsString: String?
)