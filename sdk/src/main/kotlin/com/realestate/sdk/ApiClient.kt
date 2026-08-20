package com.realestate.sdk

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ApiClient private constructor(
  val apiService: ApiService
) {
  companion object {
    fun create(baseUrl: String, apiKey: String? = null, bearerToken: String? = null, timeoutSeconds: Long = 30): ApiClient {
      val moshi = Moshi.Builder().build()

      val clientBuilder = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)

      if (!apiKey.isNullOrBlank()) {
        clientBuilder.addInterceptor { chain ->
          val request = chain.request().newBuilder()
            .addHeader("X-API-Key", apiKey)
            .build()
          chain.proceed(request)
        }
      }

      if (!bearerToken.isNullOrBlank()) {
        val token = bearerToken
        clientBuilder.addInterceptor { chain ->
          val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ".plus(token))
            .build()
          chain.proceed(request)
        }
      }

      val logging = HttpLoggingInterceptor()
      logging.level = HttpLoggingInterceptor.Level.BASIC
      clientBuilder.addInterceptor(logging)

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
