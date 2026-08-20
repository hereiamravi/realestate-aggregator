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

    private var apiClient: ApiClient? = null

    fun initClient(baseUrl: String, apiKey: String?) {
        apiClient = ApiClient.create(baseUrl = baseUrl, apiKey = apiKey)
    }

    fun loadFeed() {
        val client = apiClient ?: return
        viewModelScope.launch(ioDispatcher) {
            try {
                val resp = client.apiService.getFeed(pageSize = 30)
                if (resp.isSuccessful) {
                    val list = resp.body()?.items ?: emptyList()
                    _posts.postValue(list)
                } else {
                    // error handling: could expose an error LiveData
                }
            } catch (ex: Exception) {
                // network or parsing error: log or expose to UI
            }
        }
    }
}
