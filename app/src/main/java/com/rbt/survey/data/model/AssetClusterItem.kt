package com.rbt.survey.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class AssetClusterItem(
    val latLng: LatLng,
    val asset: CreatedAssetData
) : ClusterItem {

    override fun getPosition(): LatLng = latLng

    override fun getTitle(): String {
        return asset.data?.get("assetName")?.asString ?: ""
    }

    override fun getSnippet(): String {
        return asset.assetId.toString()
    }

    override fun getZIndex(): Float = 0f
}