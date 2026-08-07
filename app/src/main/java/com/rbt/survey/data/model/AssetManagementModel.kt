package com.rbt.survey.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject


data class ProjectResponse(
    val projectId: Int,
    val projectCode: String,
    val projectName: String,
    val isActive: Boolean
)

data class ConnectivityRuleResponse(
    val ruleId: Int,
    val tenantId: Int,
    val parentAssetTypeId: Int,
    val parentAssetTypeCode: String?,
    val parentAssetTypeName: String?,
    val childAssetTypeId: Int,
    val childAssetTypeCode: String?,
    val childAssetTypeName: String?,
    val relationshipType: String?,
    val isBidirectional: Boolean
)

data class AssetTypeResponse(
    val assetCode: String,
    val assetName: String,
    val assetTypeID: Int
)

data class CreateAssetRequest(
    val assetCode: String,
    val assetName: String,
    val assetCategory: String,
    val assetnature: String,
    val geometryType: String
)

data class AssetDetailResponse(
    val success: Boolean,
    val message: String?,
    val data: AssetDetailsData,
    val errors: Any?
)

data class AssetDetailsData(
    val assetTypeId: Int,
    val assetCode: String,
    val assetName: String,
    val assetCategory: String,
    val imageUrl: String?,
    val isActive: Boolean,
    val geometryType: String,
    val fields: List<Any>
)

data class CreatedAssetDetailResponse(
    val success: Boolean,
    val message: String?,
    val data: CreatedAssetDetailsData,
    val errors: Any?
)

data class CreatedAssetDetailsData(
    val portUtilization: List<PortUtilization> = emptyList(),
    val coreUtilization: List<Any> = emptyList(),
    val assetDynamicFieldValueId: Int,
    val assetId: Int,
    val projectId: Int,
    val assetTypeId: Int,
    val assetCode: String,
    val configVersionId: Int,
    val configVersionNo: Int,
    val data: Map<String, JsonElement>?,
    val dropdownSnapshot: JsonElement?,
    val createdOn: String?,
    val updatedOn: String?,
    val createdBy: String?,
    val updatedBy: String?,
    val files: List<AssetFile>?,
    val asset: CreatedAsset?,
    val assetGeometry: AssetGeometry?,
    val parentAssetId: Int?
)

data class AssetFile(
    val fileId: Int?,
    val assetId: Int?,
    val fieldId: String?,
    val originalFileName: String?,
    val storedFileName: String?,
    val relativePath: String?,
    val contentType: String?,
    val fileSize: Long?,
    val createdOn: String?,
    val uploadedBy: String?
)
data class CreatedAsset(
    val assetId: Int,
    val tenantId: Int,
    val projectId: Int,
    val regionId: Int?,
    val assetTypeId: Int,
    val assetCode: String?,
    val assetName: String?,
    val status: String?,
    val installedOn: String?,
    val retiredOn: String?,
    val createdOn: String?,
    val updatedOn: String?,
    val isDeleted: Boolean,
    val parentAssetId: Int?,
    val description: String?
)
data class AssetGeometry(
    val assetId: Int,
    val wkt: String?,
    val srid: Int?
)


data class FormFieldMasterResponse(
    val success: Boolean,
    val message: String?,
    val data: List<FieldMaster>,
    val errors: Any?
)

data class FieldMaster(
    val fieldMasterId: Int,
    val fieldCode: String,
    val fieldName: String,
    val fieldType: String,
    val category: String,
    val template: JsonObject,
    val isActive: Boolean,
    val createdOn: String,
    val updatedOn: String?
)


data class AssetConfigResponse(
    val success: Boolean,
    val message: String?,
    val data: DynamicFieldConfigData?,
    val errors: Any?
)

data class DynamicFieldConfigData(
    val assetDynamicFieldConfigId: Int,
    val assetTypeId: Int,
    val currentVersionId: Int,
    val currentVersionNo: Int,
    val configMetadata: ConfigMetadata,
    val layout: Layout,
    val isActive: Boolean,
    val createdOn: String,
    val updatedOn: String?,
    val currentVersion: CurrentVersion
)

data class ConfigMetadata(
    val module: String,
    val source: String,
    val assetCode: String
)

data class Layout(
    val sections: List<LayoutSection>
)

data class LayoutSection(
    val title: String,
    val fields: List<String>
)

data class CurrentVersion(
    val assetDynamicFieldConfigVersionId: Int,
    val assetDynamicFieldConfigId: Int,
    val assetTypeId: Int,
    val versionNo: Int,
    val schema: Schema,
    val configMetadata: ConfigMetadata,
    val layout: Layout,
    val isPublished: Boolean,
    val createdBy: String,
    val createdOn: String
)

data class Schema(
    val fields: List<DynamicField>
)

data class DynamicField(
    val id: String,
    val type: String,
    val label: String,
    val multi: Boolean,
    val options: List<FieldOption> = emptyList(),
    val isActive: Boolean,
    val readOnly: Boolean,
    val required: Boolean,
    val dataSource: JsonObject? = null,
    val searchable: Boolean,
    val validation: JsonObject? = null,
    val placeholder: String?,
    val resetOnHide: Boolean,
    val defaultValue: Any?,
    val dependencies: List<FieldsDependency> = emptyList(),
    val legacyFieldId: Int?,
    val masterFieldId: Int,
    val dependentOptions: JsonObject? = null,
    val masterColumnName: String?,
    val resolveFromMaster: Boolean,
    val conditionalLogic: assetConditionalLogic? = null
)

data class FieldOption(
    val label: String,
    val value: String
)

data class FieldsDependency(
    val field: String,
    val required: Boolean,
    val clearOnParentChange: Boolean
)


data class OptionItem(
    var value: String = "",
    var label: String = ""
)

data class assetConditionalLogic(
    val logic: String,
    val conditions: List<assetCondition>
)

data class assetCondition(
    val field: String,
    val value: String,
    val operator: String
)


data class SaveConfigRequest(
    val schema: SaveSchema,
    val configMetadata: SaveConfigMetadata,
    val layout: SaveLayout,
    val isPublished: Boolean,
    val createdBy: String
)

data class SaveSchema(
    val fields: List<DynamicField>
)

data class SaveConfigMetadata(
    val module: String,
    val source: String,
    val assetCode: String
)

data class SaveLayout(
    val sections: List<SaveSection>
)

data class SaveSection(
    val title: String,
    val fields: List<String>
)


data class CreateAddAssetRequest(
    val projectId: Int,
    val regionId: Int,
    val assetTypeId: Int,
    val assetCode: String,
    val assetName: String,
    val description: String,
    val status: String,
    val installedOn: String?,
    val ParentAssetId: Int?,
    val fields: List<Any>,
    val dynamicFields: DynamicFieldsRequest,
    val geometry: GeometryRequest
)

data class GeometryRequest(
    val wkt: String,
    val srid: Int = 4326
)

data class DynamicFieldsRequest(
    val data: Map<String, Any?>,
    val dropdownSnapshot: Map<String, DropdownSnapshot>,
    val uploadedFileIds: List<Int> = emptyList(),
    val uploadedFiles: List<Any> = emptyList(),
    val updatedBy: String = "admin",
    val attemptedUploadCount: Int = 0,
    val successfulUploadCount: Int = 0
)

data class DropdownSnapshot(
    val value: String,
    val label: String
)

data class UpdateDynamicFieldsRequest(
    val data: Map<String, Any?>,
    val dropdownSnapshot: Map<String, DropdownSnapshot>,
    val uploadedFileIds: List<Int> = emptyList(),
    val uploadedFiles: List<Any> = emptyList(),
    val updatedBy: String = "admin",
    val attemptedUploadCount: Int = 0,
    val successfulUploadCount: Int = 0,
    val useCurrentVersion: Boolean = false
)

//data class CreatedAssetsResponse(
//    val success: Boolean,
//    val message: String?,
//    val data: List<CreatedAssetData>,
//    val errors: Any?
//)
//data class CreatedAssetData(
//    val assetDynamicFieldValueId: Int,
//    val assetId: Int,
//    val projectId: Int,
//    val assetTypeId: Int,
//    val assetCode: String,
//    val configVersionId: Int,
//    val configVersionNo: Int,
//    val data: Map<String, JsonElement>?,
//    val dropdownSnapshot: JsonElement?,
//    val createdOn: String?,
//    val updatedOn: String?,
//    val createdBy: String?,
//    val updatedBy: String?,
//    val files: List<AssetFile>?,
//    val asset: CreatedAsset?,
//    val assetGeometry: AssetGeometry?,
//    val parentAssetId: Int?
//)
//data class createdDropdownSnapshot(
//    val label: String?,
//    val value: String?
//)
//data class AssetFile(
//    val fileId: Int?,
//    val assetId: Int?,
//    val fieldId: String?,
//    val originalFileName: String?,
//    val storedFileName: String?,
//    val relativePath: String?,
//    val contentType: String?,
//    val fileSize: Long?,
//    val createdOn: String?,
//    val uploadedBy: String?
//)
//data class CreatedAsset(
//    val assetId: Int,
//    val tenantId: Int,
//    val projectId: Int,
//    val regionId: Int?,
//    val assetTypeId: Int,
//    val assetCode: String?,
//    val assetName: String?,
//    val status: String?,
//    val installedOn: String?,
//    val retiredOn: String?,
//    val createdOn: String?,
//    val updatedOn: String?,
//    val isDeleted: Boolean,
//    val parentAssetId: Int?,
//    val description: String?
//)
//data class AssetGeometry(
//    val assetId: Int,
//    val wkt: String?,
//    val srid: Int?
//)

data class CreatedAssetsResponse(
    val success: Boolean,
    val message: String?,
    val data: List<CreatedAssetData>,
    val errors: Any?
)

data class CreatedAssetData(
    val assetId: Int,
    val assetTypeId: Int,
    val assetCode: String,
    val data: Map<String, JsonElement>?,
)


data class LocationKey(
    val lat: Double,
    val lng: Double
)


data class CustomerResponse(
    val customerId: Int,
    val customerName: String,
    val companyName: String,
    val customerType: String,
    val status: String,
    val entryOn: String,
    val updatedOn: String?,
    val updatedBy: String
)


data class FmsPortUtilizationResponse(
    val utilizationStatus: String?,
    val terminationId: Int?,
    val pairId: Int?,
    val pairedCoreId: Int?,
    val pairNo: Int?,
    val portId: Int?,
    val assetId: Int?,
    val portNo: Int?,
    val portType: String?,
    val status: String?,
    val healthStatus: String?,
    val createdOn: String?,
    val updatedOn: String?
)



data class FiberStructureResponse(
    val fiberAssetId: Int?,
    val tubes: List<FiberTubeStructure>?
)

data class FiberTubeStructure(
    val tubeId: Int?,
    val tubeNo: Int?,
    val hexCode: String?,
    val status: String?,
    val createdOn: String?,
    val cores: List<FiberCoreStructure>?
)

data class FiberCoreStructure(
    val coreId: Int?,
    val coreNo: Int?,
    val hexCode: String?,
    val status: String?,
    val createdOn: String?
)



data class FiberCoreUtilizationResponse(
    val coreId: Int?,
    val tubeId: Int?,
    val tubeNo: Int?,
    val coreNo: Int?,
    val cableEndId: Int?,
    val cableEndCode: String?,
    val attachedAssetId: Int?,
    val coreStatus: String?,
    val utilizationStatus: String?,
    val spliceId: Int?,
    val terminationId: Int?,
    val utilizationType: String?,
    val pairId: Int?,
    val pairedCoreId: Int?,
    val pairedPortId: Int?,
    val pairNo: Int?,
    val pairSide: String?
)



data class PortUtilization(
    val utilizationStatus: String?,
    val terminationId: Int?,
    val pairId: Int?,
    val pairedCoreId: Int?,
    val pairNo: Int?,
    val portId: Int?,
    val assetId: Int?,
    val portNo: Int?,
    val portType: String?,
    val status: String?,
    val healthStatus: String?,
    val createdOn: String?,
    val updatedOn: String?
)