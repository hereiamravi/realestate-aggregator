package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Channel(
    val id: String,
    val instagram_handle: String,
    val display_name: String? = null,
    val profile_url: String? = null,
    val source: String,
    val enabled: Boolean? = true,
    val last_fetched_at: String? = null,
    val metadata: Map<String, Any>? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
