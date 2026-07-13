package com.rbt.survey.data.model

import com.google.gson.JsonObject

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