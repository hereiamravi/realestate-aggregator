package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Bookmark(
    val id: String,
    val user_id: String,
    val post_id: String,
    val created_at: String
)
