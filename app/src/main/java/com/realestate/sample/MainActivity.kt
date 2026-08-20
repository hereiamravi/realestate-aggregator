package com.realestate.sample

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.realestate.sdk.ApiClient
import com.realestate.sdk.models.Post

class MainActivity : ComponentActivity() {
    private val viewModel: FeedViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = FeedAdapter { post -> openInstagram(post) }
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

        viewModel.posts.observe(this) { list ->
            adapter.submitList(list)
        }

        // Configure your API base URL and API key here for the sample
        // For emulator: use http://10.0.2.2:8080/ to reach the host machine where mock server runs
        viewModel.initClient(baseUrl = "http://10.0.2.2:8080/", apiKey = "YOUR_CLIENT_API_KEY")
        viewModel.loadFeed()
    }

    private fun openInstagram(post: Post) {
        val url = post.original_url ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "Cannot open Instagram", Toast.LENGTH_SHORT).show()
        }
    }
}
