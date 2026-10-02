package com.realestate.sample

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.sdk.ApiClient
import com.realestate.sdk.models.Post
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedViewModel(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private var apiClient: ApiClient? = null
    private var currentCursor: String? = null
    private var hasLoadedInitial = false

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    fun initClient(baseUrl: String, apiKey: String?) {
        apiClient = ApiClient.create(baseUrl = baseUrl, apiKey = apiKey)
    }

    fun loadFeed() {
        // Reset for refresh
        currentCursor = null
        hasLoadedInitial = false
        _hasMore.postValue(true)
        _posts.postValue(emptyList())
        loadMore(false) // false = not showing loading indicator for initial load
    }

    fun loadMore(showLoadingIndicator: Boolean = true) {
        val client = apiClient ?: return

        // Prevent multiple simultaneous loads or loading past the end
        if (_isLoading.value == true) return
        if (hasLoadedInitial && _hasMore.value == false) return

        viewModelScope.launch(ioDispatcher) {
            try {
                if (showLoadingIndicator) {
                    _isLoading.postValue(true)
                }

                val resp = client.apiService.getFeed(
                    pageSize = 30,
                    cursor = currentCursor
                )

                if (resp.isSuccessful) {
                    val response = resp.body()
                    if (response != null) {
                        val newItems = response.items

                        // Update cursor for next page
                        currentCursor = response.next_cursor
                        _hasMore.postValue(response.next_cursor != null)

                        // Append without duplicates (by post id)
                        val currentList = _posts.value.orEmpty()
                        val existingIds = currentList.map { it.id }.toHashSet()
                        val deduped = newItems.filterNot { existingIds.contains(it.id) }
                        // If this is a refresh (cursor was null at request time) and list was cleared,
                        // currentList is empty so this is just the first page.
                        _posts.postValue(currentList + deduped)
                        hasLoadedInitial = true
                    }
                } else {
                    // Handle error - could expose error state
                }
            } catch (ex: Exception) {
                // Handle network/parsing error
            } finally {
                if (showLoadingIndicator) {
                    _isLoading.postValue(false)
                }
            }
        }
    }
}
