package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaItem(
    val url: String,
    val type: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)
