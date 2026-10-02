package com.realestate.sample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val caption: String? = null,
    val thumbnailUrl: String? = null,
    val propertyType: String? = null,
    val priceRaw: String? = null,
    val channelId: String? = null,
    val originalUrl: String? = null,
    val instagramPostId: String? = null,
    val shortcode: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
