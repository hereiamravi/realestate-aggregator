package com.realestate.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.realestate.sdk.ApiClient
import com.realestate.sdk.models.Post
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: FeedViewModel by viewModels()
    private val bookmarksViewModel: BookmarksViewModel by viewModels()
    private lateinit var adapter: FeedAdapter
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        adapter = FeedAdapter(
            onClick = { post -> openInstagram(post) },
            onBookmarkClick = { post -> bookmarksViewModel.toggle(post) }
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

        // Observe posts list + cache for offline viewing
        viewModel.posts.observe(this) { list ->
            adapter.submitList(list)
            bookmarksViewModel.cachePosts(list)
            // Offline fallback: if network returned nothing, show last cached feed
            if (list.isEmpty()) {
                lifecycleScope.launch {
                    val cached = bookmarksViewModel.cachedPostsForOffline()
                    if (cached.isNotEmpty()) adapter.submitList(cached)
                }
            }
        }

        bookmarksViewModel.bookmarkedIds.observe(this) { ids ->
            adapter.setBookmarkedIds(ids)
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }

        // Set up scroll listener for infinite scrolling (threshold-based)
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return // only trigger when scrolling down
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()
                // Prefetch when within 5 items of the end
                if (total > 0 && lastVisible >= total - 5) {
                    loadMoreIfNeeded()
                }
            }
        })

        // Configure your API base URL and API key here for the sample
        // For emulator: use http://10.0.2.2:8080/v1/ to reach the host mock server (serves /v1/feed)
        val client = ApiClient.create(baseUrl = "http://10.0.2.2:8080/v1/", apiKey = "YOUR_CLIENT_API_KEY")
        viewModel.initClient(baseUrl = "http://10.0.2.2:8080/v1/", apiKey = "YOUR_CLIENT_API_KEY")
        bookmarksViewModel.initClient(client)
        bookmarksViewModel.refresh()
        viewModel.loadFeed()
    }

    private fun loadMoreIfNeeded() {
        // Only load more if we have data, not already loading, and more pages exist
        if (viewModel.posts.value?.isNotEmpty() == true &&
            viewModel.isLoading.value != true &&
            viewModel.hasMore.value != false) {
            viewModel.loadMore()
        }
    }

    private fun openInstagram(post: Post) {
        InstagramDeepLink.openPost(this, post)
    }
}
