plugins {
    id("com.android.application") version "8.13.2"
    kotlin("android") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}

android {
    namespace = "com.realestate.sample"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.realestate.sample"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/manifest/AndroidManifest.xml")
        }
    }
}

dependencies {
    implementation(project(":sdk"))

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha01")
    implementation("com.google.android.material:material:1.9.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.8.0")

    implementation("androidx.recyclerview:recyclerview:1.3.0")

    implementation("io.coil-kt:coil:2.4.0")

    // Coroutines (should be already in sdk but app may use lifecycleScope)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Room for bookmarks + offline feed cache (TODO item H)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
