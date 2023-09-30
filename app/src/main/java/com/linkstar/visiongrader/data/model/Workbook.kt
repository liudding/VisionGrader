package com.linkstar.visiongrader.data.model

import com.google.gson.annotations.SerializedName

data class Workbook(
    @SerializedName("guuid") val id: String,
    @SerializedName("tngCaseName") val name: String
)
