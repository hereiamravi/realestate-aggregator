package com.realestate.sdk

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.*

/*
  Compact Kotlin client SDK for the RealEstate Instagram Feed Aggregator
  - Uses Retrofit + Moshi
  - Single-file distribution for a quick start; copy into your Android project under appropriate package
  - Provides ApiClient builder and ApiService interface matching the OpenAPI contract
*/

// -----------------------------
// Models
// -----------------------------

@JsonClass(generateAdapter = true)
data class MediaItem(
    val url: String,
    val type: String,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null
)

@JsonClass(generateAdapter = true)
data class PriceObject(
    val raw: String? = null,
    val value: Double? = null,
    val currency: String? = null
)

@JsonClass(generateAdapter = true)
data class ExtractedFields(
    val property_type: String? = null,
    val price: PriceObject? = null,
    val location: String? = null,
    val area: String? = null
)

@JsonClass(generateAdapter = true)
data class Channel(
    val id: String,
    val instagram_handle: String,
    val display_name: String? = null,
    val profile_url: String? = null,
    val source: String,
    val enabled: Boolean? = true,
    val last_fetched_at: String? = null,
    val metadata: Map<String, Any>? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@JsonClass(generateAdapter = true)
data class Post(
    val id: String,
    val instagram_post_id: String? = null,
    val shortcode: String? = null,
    val channel_id: String? = null,
    val original_url: String? = null,
    val caption: String? = null,
    val media: List<MediaItem>,
    val media_type: String? = null,
    val timestamp: String? = null,
    val likes_count: Int? = null,
    val comments_count: Int? = null,
    val extracted: ExtractedFields? = null,
    val source: String? = null,
    val raw_payload: Map<String, Any>? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_removed: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class PostListResponse(
    val items: List<Post>,
    val next_cursor: String? = null,
    val page_size: Int? = null
)

@JsonClass(generateAdapter = true)
data class Bookmark(
    val id: String,
    val user_id: String,
    val post_id: String,
    val created_at: String
)

@JsonClass(generateAdapter = true)
data class FetchLog(
    val id: String,
    val channel_id: String?,
    val job_id: String?,
    val started_at: String?,
    val finished_at: String?,
    val status: String?,
    val details: Map<String, Any>?
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    val code: Int,
    val message: String
)

// -----------------------------
// Retrofit service interface
// -----------------------------
interface ApiService {
  @GET("/feed")
  fun getFeed(
    @Query("page_size") pageSize: Int = 25,
    @Query("cursor") cursor: String? = null,
    @Query("channel_id") channelId: String? = null,
    @Query("property_type") propertyType: String? = null,
    @Query("q") q: String? = null,
    @Query("from_date") fromDate: String? = null,
    @Query("to_date") toDate: String? = null,
    @Query("media_type") mediaType: String? = null
  ): Call<PostListResponse>

  @GET("/posts/{post_id}")
  fun getPost(@Path("post_id") postId: String): Call<Post>

  @GET("/channels")
  fun getChannels(@Query("include_inactive") includeInactive: Boolean? = false): Call<List<Channel>>

  @GET("/channels/{channel_id}")
  fun getChannel(@Path("channel_id") channelId: String): Call<Channel>

  @GET("/bookmarks")
  fun getBookmarks(@Query("page_size") pageSize: Int = 25, @Query("cursor") cursor: String? = null): Call<Map<String, Any>>

  @POST("/bookmarks")
  fun createBookmark(@Body body: Map<String, String>): Call<Bookmark>

  @DELETE("/bookmarks/{bookmark_id}")
  fun deleteBookmark(@Path("bookmark_id") bookmarkId: String): Call<Void>

  // Admin endpoints (require Bearer admin token)
  @POST("/admin/channels")
  fun adminCreateChannel(@Body body: Map<String, Any>): Call<Channel>

  @PATCH("/admin/channels/{channel_id}")
  fun adminUpdateChannel(@Path("channel_id") channelId: String, @Body body: Map<String, Any>): Call<Channel>

  @POST("/admin/channels/{channel_id}/fetch")
  fun adminFetchChannel(@Path("channel_id") channelId: String): Call<Map<String, String>>

  @POST("/admin/webhook")
  fun adminWebhook(@Body body: Map<String, Any>): Call<Void>

  @GET("/admin/fetch-logs")
  fun adminFetchLogs(@Query("channel_id") channelId: String? = null, @Query("status") status: String? = null): Call<List<FetchLog>>
}

// -----------------------------
// Api client builder
// -----------------------------
class ApiClient private constructor(
  val apiService: ApiService
) {
  companion object {
    fun create(baseUrl: String, apiKey: String? = null, bearerToken: String? = null, timeoutSeconds: Long = 30): ApiClient {
      val moshi = Moshi.Builder().build()

      val clientBuilder = OkHttpClient.Builder()

      // Api key interceptor for client requests
      if (!apiKey.isNullOrBlank()) {
        clientBuilder.addInterceptor { chain ->
          val request = chain.request().newBuilder()
            .addHeader("X-API-Key", apiKey)
            .build()
          chain.proceed(request)
        }
      }

      // Bearer token interceptor (admin)
      if (!bearerToken.isNullOrBlank()) {
        clientBuilder.addInterceptor { chain ->
          val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $bearerToken")
            .build()
          chain.proceed(request)
        }
      }

      val client = clientBuilder.build()

      val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()

      val service = retrofit.create(ApiService::class.java)
      return ApiClient(service)
    }
  }
}

// -----------------------------
// Simple helper exceptions
// -----------------------------
class ApiException(message: String, val code: Int): Exception(message)

// -----------------------------
// Usage example (from Android coroutine scope or background thread):
// val api = ApiClient.create("https://api.example.com/v1/", apiKey = "YOUR_KEY")
// val call = api.apiService.getFeed(pageSize = 20)
// val resp = call.execute()
// if (resp.isSuccessful) {
//   val body = resp.body()
// } else {
//   val err = resp.errorBody()?.string()
// }
// -----------------------------
