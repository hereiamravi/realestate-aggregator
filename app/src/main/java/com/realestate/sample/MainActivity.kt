package com.realestate.sample

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.realestate.sdk.ApiClient
import com.realestate.sdk.models.Post
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: FeedViewModel by viewModels()
    private val bookmarksViewModel: BookmarksViewModel by viewModels()
    private lateinit var adapter: FeedAdapter
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var rv: RecyclerView
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var activeFilterBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        activeFilterBadge = findViewById(R.id.activeFilterBadge)

        setupChipFilters()

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

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()
                if (total > 0 && lastVisible >= total - 5) {
                    loadMoreIfNeeded()
                }
            }
        })

        val client = ApiClient.create(baseUrl = "http://localhost:8080/v1/", apiKey = "YOUR_CLIENT_API_KEY")
        viewModel.initClient(baseUrl = "http://localhost:8080/v1/", apiKey = "YOUR_CLIENT_API_KEY")
        bookmarksViewModel.initClient(client)
        bookmarksViewModel.refresh()
        viewModel.loadFeed()
    }

    private fun setupChipFilters() {
        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            when (checkedIds.first()) {
                R.id.chipAll -> {
                    activeFilterBadge.text = "📍 All Cities"
                    viewModel.applyFilters("All Cities", "All Divisions", "All Areas", "All Types")
                }
                R.id.chipMumbai -> {
                    activeFilterBadge.text = "🏙️ Mumbai"
                    viewModel.applyFilters("Mumbai", "All Divisions", "All Areas", "All Types")
                }
                R.id.chipBandra -> {
                    activeFilterBadge.text = "🏠 Bandra, Mumbai"
                    viewModel.applyFilters("Mumbai", "Western Suburbs", "Bandra", "All Types")
                }
                R.id.chipPowai -> {
                    activeFilterBadge.text = "🌊 Powai, Mumbai"
                    viewModel.applyFilters("Mumbai", "Central Suburbs", "Powai", "All Types")
                }
                R.id.chipDelhi -> {
                    activeFilterBadge.text = "🏛️ Delhi"
                    viewModel.applyFilters("Delhi", "All Divisions", "All Areas", "All Types")
                }
                R.id.chipBangalore -> {
                    activeFilterBadge.text = "🌳 Bangalore"
                    viewModel.applyFilters("Bangalore", "All Divisions", "All Areas", "All Types")
                }
                R.id.chipFlats -> {
                    activeFilterBadge.text = "🏢 Flats / Apartments"
                    viewModel.applyFilters("All Cities", "All Divisions", "All Areas", "flats")
                }
                R.id.chipPlots -> {
                    activeFilterBadge.text = "🏞️ Open Plots / Land"
                    viewModel.applyFilters("All Cities", "All Divisions", "All Areas", "plots")
                }
            }
        }
    }

    private fun loadMoreIfNeeded() {
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
