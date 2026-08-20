package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FetchLog(
    val id: String,
    val channel_id: String?,
    val job_id: String?,
    val started_at: String?,
    val finished_at: String?,
    val status: String?,
    val details: Map<String, Any>?
)
