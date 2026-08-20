Sample Android app module that demonstrates use of the :sdk module.

How to run (in your Android project):
1) Open the project in Android Studio
2) Ensure Kotlin and Android Gradle Plugin versions are compatible with the module files.
3) Replace the placeholder API baseUrl and apiKey in MainActivity with your real values.
4) Build and run the :app module.

What this sample shows:
- Fetching the aggregated feed using the SDK's coroutine-based ApiService
- Displaying posts in a RecyclerView
- Clicking a post opens the original Instagram URL using an Intent (deep-link)

Notes:
- This is a minimal sample for demonstration. For production code, add proper error handling, paging, placeholder images, and better UI components (custom layout for each post).  
- Ensure the SDK module is compiled and available to the app (project(':sdk') is referenced in app/build.gradle.kts).