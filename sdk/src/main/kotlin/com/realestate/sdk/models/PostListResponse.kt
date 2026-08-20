package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostListResponse(
    val items: List<Post>,
    val next_cursor: String? = null,
    val page_size: Int? = null
)
