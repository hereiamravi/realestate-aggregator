package com.realestate.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.realestate.sdk.ApiClient
import com.realestate.sample.data.BookmarksRepository
import com.realestate.sample.data.local.AppDatabase
import com.realestate.sdk.models.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BookmarksRepository(AppDatabase.get(app))

    private val _bookmarkedIds = MutableLiveData<Set<String>>(emptySet())
    val bookmarkedIds: LiveData<Set<String>> = _bookmarkedIds

    fun initClient(client: ApiClient?) = repo.setApiClient(client)

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = repo.bookmarkedIds()
            _bookmarkedIds.postValue(ids)
        }
    }

    fun toggle(post: Post, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val nowBookmarked = repo.toggle(post)
            _bookmarkedIds.postValue(repo.bookmarkedIds())
            withContext(Dispatchers.Main) { onDone?.invoke(nowBookmarked) }
        }
    }

    fun cachePosts(posts: List<Post>) {
        if (posts.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) { repo.cachePosts(posts) }
    }

    /** Offline fallback: posts cached from previous successful feed loads. */
    suspend fun cachedPostsForOffline(): List<Post> = withContext(Dispatchers.IO) {
        repo.cachedPosts().map { c ->
            Post(
                id = c.id,
                instagram_post_id = c.instagramPostId,
                shortcode = c.shortcode,
                channel_id = c.channelId,
                original_url = c.originalUrl,
                caption = c.caption,
                media = c.thumbnailUrl?.let {
                    listOf(com.realestate.sdk.models.MediaItem(url = it, type = "image"))
                } ?: emptyList(),
                extracted = com.realestate.sdk.models.ExtractedFields(
                    property_type = c.propertyType,
                    price = c.priceRaw?.let {
                        com.realestate.sdk.models.PriceObject(raw = it, value = null, currency = null)
                    }
                )
            )
        }
    }
}
