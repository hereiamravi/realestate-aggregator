package com.realestate.sample.data

import com.realestate.sdk.ApiClient
import com.realestate.sample.data.local.AppDatabase
import com.realestate.sample.data.local.BookmarkEntity
import com.realestate.sample.data.local.CachedPostEntity
import com.realestate.sdk.models.Post

class BookmarksRepository(
    private val db: AppDatabase,
    private var apiClient: ApiClient? = null
) {
    fun setApiClient(client: ApiClient?) { apiClient = client }

    suspend fun bookmarkedIds(): Set<String> =
        db.bookmarkDao().getBookmarkedPostIds().toSet()

    suspend fun isBookmarked(postId: String): Boolean =
        db.bookmarkDao().countByPostId(postId) > 0

    /** Toggle: persist locally first (offline-safe), then best-effort sync with API. */
    suspend fun toggle(post: Post): Boolean {
        val dao = db.bookmarkDao()
        return if (dao.countByPostId(post.id) > 0) {
            dao.deleteByPostId(post.id)
            // Best-effort server delete: we don't store server bookmark id mapping
            // in this sample, so skip API call on untoggle to avoid wrong id deletes.
            false
        } else {
            dao.upsert(
                BookmarkEntity(
                    postId = post.id,
                    caption = post.caption,
                    thumbnailUrl = post.media.firstOrNull()?.url
                )
            )
            // Best-effort server create (ignore failures for offline support)
            try {
                val resp = apiClient?.apiService?.createBookmark(mapOf("post_id" to post.id))
                if (resp != null && resp.isSuccessful) {
                    resp.body()?.let { server ->
                        dao.upsert(
                            BookmarkEntity(
                                postId = post.id,
                                bookmarkId = server.id,
                                caption = post.caption,
                                thumbnailUrl = post.media.firstOrNull()?.url
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
            true
        }
    }

    suspend fun cachePosts(posts: List<Post>) {
        db.cachedPostDao().upsertAll(posts.map { p ->
            CachedPostEntity(
                id = p.id,
                caption = p.caption,
                thumbnailUrl = p.media.firstOrNull()?.url,
                propertyType = p.extracted?.property_type,
                priceRaw = p.extracted?.price?.raw,
                channelId = p.channel_id,
                originalUrl = p.original_url,
                instagramPostId = p.instagram_post_id,
                shortcode = p.shortcode
            )
        })
    }

    suspend fun cachedPosts(limit: Int = 100): List<CachedPostEntity> =
        db.cachedPostDao().getRecent(limit)

    suspend fun bookmarkedPosts(): List<BookmarkEntity> = db.bookmarkDao().getAll()
}
