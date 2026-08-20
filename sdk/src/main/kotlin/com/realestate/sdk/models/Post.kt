package com.realestate.sdk.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Post(
    val id: String,
    val instagram_post_id: String? = null,
    val shortcode: String? = null,
    val channel_id: String? = null,
    val original_url: String? = null,
    val caption: String? = null,
    val media: List<MediaItem>,
    val media_type: String? = null,
    val timestamp: String? = null,
    val likes_count: Int? = null,
    val comments_count: Int? = null,
    val extracted: ExtractedFields? = null,
    val source: String? = null,
    val raw_payload: Map<String, Any>? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_removed: Boolean? = false
)
