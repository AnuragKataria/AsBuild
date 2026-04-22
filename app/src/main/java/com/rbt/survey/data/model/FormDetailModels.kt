package com.rbt.survey.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class FormDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: FormDetailData?,
    @SerializedName("errors") val errors: Any?
)

data class FormDetailData(
    @SerializedName("formId") val formId: Int,
    @SerializedName("formCode") val formCode: String,
    @SerializedName("formName") val formName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("currentVersion") val currentVersion: FormVersion?,
    @SerializedName("geoContext") val geoContext: GeoContext? = null
)

data class GeoContext(
    @SerializedName("blockCode") val blockCode: String?,
    @SerializedName("levels") val levels: List<GeoLevel>?
)

data class GeoLevel(
    @SerializedName("levelCode") val levelCode: String,
    @SerializedName("levelName") val levelName: String,
    @SerializedName("selectedValue") val selectedValue: String?,
    @SerializedName("selectedLabel") val selectedLabel: String?,
    @SerializedName("isTerminal") val isTerminal: Boolean,
    @SerializedName("options") val options: List<FormOption>?
)

data class FormOption(
    @SerializedName("Label", alternate = ["label"]) val label: String,
    @SerializedName("Value", alternate = ["value"]) val value: String,
    @SerializedName("raw")    val raw: Map<String, Any>? = null
)

data class FormVersion(
    @SerializedName("formVersionId") val formVersionId: Int,
    @SerializedName("versionNo") val versionNo: Int,
    @SerializedName("schema") val schema: FormSchema?
)

data class FormSchema(
    @SerializedName("fields") val fields: List<FormField>
)

data class FormField(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String,
    @SerializedName("options") val options: List<Any>?, // Changed to Any to handle String or Map
    @SerializedName("required") val required: Boolean,
    @SerializedName("masterFieldId") val masterFieldId: Int?,
    @SerializedName("dependencies") val dependencies: List<FieldDependency>?,
//    @SerializedName("dependentOptions") val dependentOptions: Map<String, Map<String, List<FormOption>>>?,
    @SerializedName("dependentOptions") val dependentOptions: Any?,
    @SerializedName("multi") val multi: Boolean? = false,
    @SerializedName("defaultValue") val defaultValue: Any? = null,
    @SerializedName("validation") val validation: FieldValidation? = null,
    @SerializedName("placeholder") val placeholder: String? = null,
    @SerializedName("mapConfig") val mapConfig: MapConfig? = null,
    @SerializedName("geometryType") val geometryType: List<String>? = null,
    @SerializedName("readOnly") val readOnly: Boolean? = false,
    @SerializedName("conditionalLogic") val conditionalLogic: ConditionalLogic? = null,
    @SerializedName("resetOnHide") val resetOnHide: Boolean? = false
)

data class ConditionalLogic(
    @SerializedName("logic") val logic: String,
    @SerializedName("conditions") val conditions: List<Condition>
)

data class Condition(
    @SerializedName("field") val field: String,
    @SerializedName("value") val value: String,
    @SerializedName("operator") val operator: String
)

data class FieldValidation(
    @SerializedName("max") val max: Double? = null,
    @SerializedName("min") val min: Double? = null,
    @SerializedName("maxLength") val maxLength: Int? = null,
    @SerializedName("minLength") val minLength: Int? = null,
    @SerializedName("pattern") val pattern: String? = null
)

data class MapConfig(
    @SerializedName("editable") val editable: Boolean = true,
    @SerializedName("defaultZoom") val defaultZoom: Int = 14,
    @SerializedName("defaultCenter") val defaultCenter: MapLatLng? = null
)

data class MapLatLng(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class FieldDependency(
    @SerializedName("field") val field: String,
    @SerializedName("required") val required: Boolean,
    @SerializedName("clearOnParentChange") val clearOnParentChange: Boolean
)
