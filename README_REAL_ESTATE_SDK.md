RealEstate Android Client SDK (Kotlin)

This repository includes a compact Kotlin client for the RealEstate Instagram Feed Aggregator API.

Files created:
- realestate-sdk-client.kt - single-file SDK with models, ApiService (Retrofit) and ApiClient builder.

Quick start (Android / Kotlin)

1) Add dependencies (Gradle):

Add to your module build.gradle:

dependencies {
  implementation "com.squareup.retrofit2:retrofit:2.9.0"
  implementation "com.squareup.retrofit2:converter-moshi:2.9.0"
  implementation "com.squareup.moshi:moshi:1.14.0"
  implementation "com.squareup.okhttp3:okhttp:4.11.0"
  implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
  implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
}

2) Copy realestate-sdk-client.kt into your Android project under package com.realestate.sdk (or edit package at top of file).

3) Instantiate the client:

val api = ApiClient.create(
  baseUrl = "https://api.example.com/v1/",
  apiKey = "YOUR_CLIENT_API_KEY"
)

4) Call endpoints using Kotlin coroutines (recommended):

// From a coroutine scope (e.g., lifecycleScope or viewModelScope):
suspend fun loadFeed(api: com.realestate.sdk.ApiClient) {
  val response = api.apiService.getFeed(pageSize = 20)
  if (response.isSuccessful) {
    val posts = response.body()?.items ?: emptyList()
    // update UI on main thread via dispatcher or LiveData
  } else {
    // handle error: response.code(), response.errorBody()
  }
}

// Example invocation from a ViewModel:
// viewModelScope.launch { loadFeed(api) }

Notes and next steps:
- The file is intentionally compact to bootstrap development quickly. For production consider splitting models into separate files and adding more robust error handling, retry, and coroutine support (use Retrofit + Kotlin coroutines adapter).
- For secure token handling, obtain short-lived tokens from your backend rather than embedding long-lived keys in the client.
- Consider generating a fully-typed SDK with openapi-generator if you want complete parity with the OpenAPI (this file implements the common client-facing endpoints).

If you want, next step can be:
- Generate a full OpenAPI-generated Kotlin SDK (requires openapi-generator tooling) OR
- Convert this client to suspend functions using Retrofit coroutine adapter OR
- Create an Android sample app that demonstrates feed display + "Open in Instagram" deep link
