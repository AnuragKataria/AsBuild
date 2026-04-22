package com.rbt.survey.data.model

import com.google.gson.annotations.SerializedName

data class BlockAssignmentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: BlockAssignmentData?,
    @SerializedName("errors") val errors: Any?
)

data class BlockAssignmentData(
    @SerializedName("userId") val userId: Int,
    @SerializedName("assignments") val assignments: List<Assignment>
)

data class Assignment(
    @SerializedName("userBlockAssignmentId") val userBlockAssignmentId: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("blockCode") val blockCode: String,
    @SerializedName("blockName") val blockName: String?,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("assignedBy") val assignedBy: Int,
    @SerializedName("assignedOn") val assignedOn: String,
    @SerializedName("revokedOn") val revokedOn: String?
)

data class BlockSummaryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<BlockSummary>?,
    @SerializedName("errors") val errors: Any?
)

data class BlockSummary(
    @SerializedName("blockCode") val blockCode: String,
    @SerializedName("blockName") val blockName: String,
    @SerializedName("completedPercentage") val completedPercentage: Int,
    @SerializedName("pendingPercentage") val pendingPercentage: Int,
    @SerializedName("totalGP") val totalGP: Int,
    @SerializedName("gpList") val gpList: List<GpItem>
)

data class GpItem(
    @SerializedName("lgdCode") val lgdCode: String,
    @SerializedName("gpName") val gpName: String,
    @SerializedName("isCompleted") val isCompleted: Boolean
)
