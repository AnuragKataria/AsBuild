package com.rbt.survey.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class OptionSegmentResponse(
    @SerializedName("tncT_PROJECT_NAME") val tncT_PROJECT_NAME: String?,
    @SerializedName("optioN_CODE") val optioN_CODE: String?,
    @SerializedName("spaN_ID") val spaN_ID: String?,
    @SerializedName("rinG_NO") val rinG_NO: String?,
    @SerializedName("rinG_CODE") val rinG_CODE: String?,
    @SerializedName("logicaL_RING_CODE") val logicaL_RING_CODE: String?,
    @SerializedName("networK_TYPE") val networK_TYPE: String?,
    @SerializedName("spaN_TYPE") val spaN_TYPE: String?,
    @SerializedName("topology") val topology: String?,
    @SerializedName("fromgp") val fromgp: String?,
    @SerializedName("togp") val togp: String?,
    @SerializedName("from_lgdcode") val from_lgdcode: String?,
    @SerializedName("to_lgdcode") val to_lgdcode: String?,
    @SerializedName("lengtH_IN_METERS") val lengtH_IN_METERS: Double?,
    @SerializedName("geometry") val geometry: String?,
    @SerializedName("scope") val scope: String?
)

data class MapLineItem(
    val fromGp: String,
    val toGp: String,
    val points: List<LatLng>,
    val color: Int
)