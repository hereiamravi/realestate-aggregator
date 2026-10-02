package com.realestate.sample

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.realestate.sdk.ApiClient
import com.realestate.sdk.models.ExtractedFields
import com.realestate.sdk.models.MediaItem
import com.realestate.sdk.models.Post
import com.realestate.sdk.models.PriceObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedViewModel(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>(getInitialMockPosts())
    val posts: LiveData<List<Post>> = _posts
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private var apiClient: ApiClient? = null
    private var currentCursor: String? = null
    private var hasLoadedInitial = false

    private val _hasMore = MutableLiveData(true)
    val hasMore: LiveData<Boolean> = _hasMore

    init {
        // Ensure initial mock posts are cached on start
    }

    fun initClient(baseUrl: String, apiKey: String?) {
        apiClient = ApiClient.create(baseUrl = baseUrl, apiKey = apiKey)
    }

    fun loadFeed() {
        currentCursor = null
        hasLoadedInitial = false
        _hasMore.postValue(true)
        loadMore(false)
    }

    fun loadMore(showLoadingIndicator: Boolean = true) {
        val client = apiClient ?: run {
            Log.e("FeedViewModel", "ApiClient is null! Using local mock posts.")
            return
        }

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
                    if (response != null && response.items.isNotEmpty()) {
                        val newItems = response.items
                        currentCursor = response.next_cursor
                        _hasMore.postValue(response.next_cursor != null)

                        val currentList = _posts.value.orEmpty()
                        val existingIds = currentList.map { it.id }.toHashSet()
                        val deduped = newItems.filterNot { existingIds.contains(it.id) }
                        
                        // If it's a fresh load, replace with fetched items; otherwise append
                        if (currentCursor == null && currentList.size <= 2) {
                            _posts.postValue(newItems)
                        } else {
                            _posts.postValue(currentList + deduped)
                        }
                        hasLoadedInitial = true
                    }
                } else {
                    Log.e("FeedViewModel", "API Error: ${resp.code()} ${resp.errorBody()?.string()}. Keeping local mock posts.")
                }
            } catch (ex: Exception) {
                Log.e("FeedViewModel", "Exception loading feed: ${ex.message}. Keeping local mock posts.", ex)
            } finally {
                if (showLoadingIndicator) {
                    _isLoading.postValue(false)
                }
            }
        }
    }

    companion object {
        fun getInitialMockPosts(): List<Post> {
            return listOf(
                Post(
                    id = "11111111-1111-4111-8111-111111111111",
                    caption = "Open plot for sale near Lakeview. Price: ₹25L per cent. Contact: 99999...",
                    original_url = "https://www.instagram.com/p/CvXyZ_AbC/",
                    media = listOf(MediaItem(url = "https://picsum.photos/seed/lakeview-plot/1080/1080", type = "image")),
                    extracted = ExtractedFields(
                        property_type = "plots",
                        price = PriceObject(raw = "₹25L per cent", value = 2500000.0, currency = "INR"),
                        location = "Lakeview"
                    )
                ),
                Post(
                    id = "22222222-2222-4222-8222-222222222222",
                    caption = "Spacious 2BHK flat available near Downtown. Price: ₹75L. Contact: 88888...",
                    original_url = "https://www.instagram.com/p/DqAbC_DeF/",
                    media = listOf(MediaItem(url = "https://picsum.photos/seed/downtown-2bhk/800/600", type = "image")),
                    extracted = ExtractedFields(
                        property_type = "flats",
                        price = PriceObject(raw = "₹75L", value = 7500000.0, currency = "INR"),
                        location = "Downtown"
                    )
                )
            )
        }
    }
}
