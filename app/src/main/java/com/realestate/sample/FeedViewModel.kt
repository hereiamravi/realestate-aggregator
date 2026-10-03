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

    var selectedCity: String = "All Cities"
    var selectedDivision: String = "All Divisions"
    var selectedSubUrban: String = "All Areas"
    var selectedPropertyType: String = "All Types"

    fun initClient(baseUrl: String, apiKey: String?) {
        apiClient = ApiClient.create(baseUrl = baseUrl, apiKey = apiKey)
    }

    fun applyFilters(city: String, division: String, subUrban: String, propertyType: String = "All Types") {
        selectedCity = city
        selectedDivision = division
        selectedSubUrban = subUrban
        selectedPropertyType = propertyType
        loadFeed()
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
                    cursor = currentCursor,
                    city = if (selectedCity != "All Cities") selectedCity else null,
                    division = if (selectedDivision != "All Divisions") selectedDivision else null,
                    subUrban = if (selectedSubUrban != "All Areas") selectedSubUrban else null,
                    propertyType = if (selectedPropertyType != "All Types") selectedPropertyType else null
                )

                if (resp.isSuccessful) {
                    val response = resp.body()
                    if (response != null && response.items.isNotEmpty()) {
                        val newItems = response.items
                        currentCursor = response.next_cursor
                        _hasMore.postValue(response.next_cursor != null)

                        if (currentCursor == null || !hasLoadedInitial) {
                            _posts.postValue(newItems)
                        } else {
                            val currentList = _posts.value.orEmpty()
                            val existingIds = currentList.map { it.id }.toHashSet()
                            val deduped = newItems.filterNot { existingIds.contains(it.id) }
                            _posts.postValue(currentList + deduped)
                        }
                        hasLoadedInitial = true
                    } else {
                        // Filter local mock posts if network returns empty
                        _posts.postValue(filterMockPosts(getInitialMockPosts()))
                    }
                } else {
                    Log.e("FeedViewModel", "API Error: ${resp.code()} ${resp.errorBody()?.string()}. Keeping local mock posts.")
                    _posts.postValue(filterMockPosts(getInitialMockPosts()))
                }
            } catch (ex: Exception) {
                Log.e("FeedViewModel", "Exception loading feed: ${ex.message}. Keeping local mock posts.", ex)
                _posts.postValue(filterMockPosts(getInitialMockPosts()))
            } finally {
                if (showLoadingIndicator) {
                    _isLoading.postValue(false)
                }
            }
        }
    }

    private fun filterMockPosts(all: List<Post>): List<Post> {
        var items = all
        if (selectedCity != "All Cities") {
            items = items.filter { it.caption?.contains(selectedCity, ignoreCase = true) == true || it.extracted?.location?.contains(selectedCity, ignoreCase = true) == true }
        }
        if (selectedSubUrban != "All Areas") {
            items = items.filter { it.caption?.contains(selectedSubUrban, ignoreCase = true) == true || it.extracted?.location?.contains(selectedSubUrban, ignoreCase = true) == true }
        }
        if (selectedPropertyType != "All Types") {
            items = items.filter { it.extracted?.property_type?.equals(selectedPropertyType, ignoreCase = true) == true || it.caption?.contains(selectedPropertyType, ignoreCase = true) == true }
        }
        return if (items.isNotEmpty()) items else all
    }

    companion object {
        fun getInitialMockPosts(): List<Post> {
            return listOf(
                Post(
                    id = "11111111-1111-4111-8111-111111111111",
                    caption = "Open plot for sale in Lakeview, Mumbai. Gated community with 24/7 security. Contact: 99999...",
                    original_url = "https://www.instagram.com/p/CvXyZ_AbC/",
                    media = listOf(MediaItem(url = "https://picsum.photos/seed/lakeview-plot/1080/1080", type = "image")),
                    extracted = ExtractedFields(
                        property_type = "plots",
                        price = PriceObject(raw = "₹25 Lakhs", value = 2500000.0, currency = "INR"),
                        location = "Lakeview, Mumbai",
                        area = "200 sq yards"
                    )
                ),
                Post(
                    id = "22222222-2222-4222-8222-222222222222",
                    caption = "Luxury 2BHK flat available in Bandra West, Mumbai. Sea view balcony, modern amenities. Contact: 88888...",
                    original_url = "https://www.instagram.com/p/DqAbC_DeF/",
                    media = listOf(MediaItem(url = "https://picsum.photos/seed/downtown-2bhk/800/600", type = "image")),
                    extracted = ExtractedFields(
                        property_type = "flats",
                        price = PriceObject(raw = "₹75 Lakhs", value = 7500000.0, currency = "INR"),
                        location = "Bandra, Mumbai",
                        area = "900 sq ft"
                    )
                ),
                Post(
                    id = "33333333-3333-4333-8333-333333333333",
                    caption = "3BHK Apartment in Hiranandani, Powai, Mumbai. Fully furnished with swimming pool & gym access.",
                    original_url = "https://www.instagram.com/p/E1Fgh_IjK/",
                    media = listOf(MediaItem(url = "https://picsum.photos/seed/powai-3bhk/1080/1080", type = "image")),
                    extracted = ExtractedFields(
                        property_type = "flats",
                        price = PriceObject(raw = "₹1.8 Crore", value = 18000000.0, currency = "INR"),
                        location = "Powai, Mumbai",
                        area = "1400 sq ft"
                    )
                ),
                Post(
                    id = "44444444-4444-4444-8444-444444444444",
                    caption = "Independent Villa for sale in Vasant Kunj, Delhi. Private garden, 4 bedrooms, prime location.",
                    original_url = "https://www.instagram.com/p/F2Ghi_LmN/",
                    media = listOf(MediaItem(url = "https://picsum.photos/seed/delhi-villa/1080/1080", type = "image")),
                    extracted = ExtractedFields(
                        property_type = "villas",
                        price = PriceObject(raw = "₹3.5 Crore", value = 35000000.0, currency = "INR"),
                        location = "Vasant Kunj, Delhi",
                        area = "2500 sq ft"
                    )
                ),
                Post(
                    id = "55555555-5555-4555-8555-555555555555",
                    caption = "Commercial Office Space in Whitefield, Bangalore. High rental yield, ready to move in.",
                    original_url = "https://www.instagram.com/p/G3Hij_OpQ/",
                    media = listOf(MediaItem(url = "https://picsum.photos/seed/bangalore-office/1080/1080", type = "image")),
                    extracted = ExtractedFields(
                        property_type = "commercial",
                        price = PriceObject(raw = "₹1.2 Crore", value = 12000000.0, currency = "INR"),
                        location = "Whitefield, Bangalore",
                        area = "1100 sq ft"
                    )
                )
            )
        }
    }
}
