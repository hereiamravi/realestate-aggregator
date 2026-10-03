package com.realestate.sdk

import com.realestate.sdk.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
  @GET("feed")
  suspend fun getFeed(
    @Query("page_size") pageSize: Int = 25,
    @Query("cursor") cursor: String? = null,
    @Query("city") city: String? = null,
    @Query("division") division: String? = null,
    @Query("sub_urban") subUrban: String? = null,
    @Query("channel_id") channelId: String? = null,
    @Query("property_type") propertyType: String? = null,
    @Query("q") q: String? = null,
    @Query("from_date") fromDate: String? = null,
    @Query("to_date") toDate: String? = null,
    @Query("media_type") mediaType: String? = null
  ): Response<PostListResponse>

  @GET("posts/{post_id}")
  suspend fun getPost(@Path("post_id") postId: String): Response<Post>

  @GET("channels")
  suspend fun getChannels(@Query("include_inactive") includeInactive: Boolean? = false): Response<List<Channel>>

  @GET("channels/{channel_id}")
  suspend fun getChannel(@Path("channel_id") channelId: String): Response<Channel>

  @GET("channels/{channel_id}/profile")
  suspend fun getChannelProfile(@Path("channel_id") channelId: String): Response<Channel>

  @GET("posts/{post_id}/comments")
  suspend fun getPostComments(@Path("post_id") postId: String): Response<Map<String, Any>>

  @GET("hashtags/search")
  suspend fun searchHashtags(@Query("q") query: String): Response<Map<String, Any>>

  @GET("locations/search")
  suspend fun searchLocations(@Query("q") query: String): Response<Map<String, Any>>

  @GET("bookmarks")
  suspend fun getBookmarks(@Query("page_size") pageSize: Int = 25, @Query("cursor") cursor: String? = null): Response<Map<String, Any>>

  @POST("bookmarks")
  suspend fun createBookmark(@Body body: Map<String, String>): Response<Bookmark>

  @DELETE("bookmarks/{bookmark_id}")
  suspend fun deleteBookmark(@Path("bookmark_id") bookmarkId: String): Response<Unit>

  // Admin endpoints
  @POST("admin/channels")
  suspend fun adminCreateChannel(@Body body: Map<String, Any>): Response<Channel>

  @PATCH("admin/channels/{channel_id}")
  suspend fun adminUpdateChannel(@Path("channel_id") channelId: String, @Body body: Map<String, Any>): Response<Channel>

  @POST("admin/channels/{channel_id}/fetch")
  suspend fun adminFetchChannel(@Path("channel_id") channelId: String): Response<Map<String, String>>

  @POST("admin/webhook")
  suspend fun adminWebhook(@Body body: Map<String, Any>): Response<Unit>

  @GET("admin/fetch-logs")
  suspend fun adminFetchLogs(@Query("channel_id") channelId: String? = null, @Query("status") status: String? = null): Response<List<FetchLog>>
}
