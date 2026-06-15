package com.rbt.survey.data.model

data class GpMapItem(
    val name: String,
    val lat: Double,
    val lng: Double,
    val lgdCode: String?,
    val isCompleted: Boolean = false
)