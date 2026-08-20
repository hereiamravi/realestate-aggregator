RealEstate SDK Module

This module provides a Kotlin/Retrofit client for the RealEstate Instagram Feed Aggregator API.

Module layout
- build.gradle.kts - Gradle build for the Android library module
- src/main/kotlin/com/realestate/sdk/ - ApiClient, ApiService
- src/main/kotlin/com/realestate/sdk/models/ - all data model classes

Usage
1) Include the module in your app's settings.gradle (this repo already includes it):
   include(":sdk")

2) In your app module's settings, add project(':sdk') as a dependency, or publish the module to your internal maven.

3) Example instantiation:

val api = ApiClient.create("https://api.example.com/v1/", apiKey = "YOUR_KEY")

// Coroutine example (launch from a ViewModelScope or other CoroutineScope)
// suspend function invocation
// val response = api.apiService.getFeed(pageSize = 20)
// if (response.isSuccessful) {
//   val posts = response.body()?.items ?: emptyList()
// }


Notes
- Models use Moshi for JSON serialization. The module uses Retrofit 2.9 and Moshi 1.14.
- For production, split models into separate files (already done here), add tests, and consider adding coroutine support (suspend functions) and proper error parsing.
