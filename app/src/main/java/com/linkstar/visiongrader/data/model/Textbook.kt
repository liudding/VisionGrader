package com.linkstar.visiongrader.data.model

import com.google.gson.annotations.SerializedName

data class Textbook(
    @SerializedName("guuid") val id: String,
    @SerializedName("bookName") val name: String
)
