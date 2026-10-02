package com.realestate.sample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val postId: String,
    val bookmarkId: String? = null, // server-side id when created via API
    val caption: String? = null,
    val thumbnailUrl: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)
